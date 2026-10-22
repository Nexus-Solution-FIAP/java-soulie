#!/usr/bin/env node
/**
 * Varredura determinística de segurança em arquivos Java deste projeto.
 *
 * Roda como hook PostToolUse em Edit|Write de **\/*.java. Não usa IA: é um
 * conjunto pequeno de regras duras, custo zero de token e resposta instantânea.
 * O raciocínio de verdade (IDOR, SSRF, abuso de fila) é do agente
 * `security-auditor`, que roda nos portões de fase.
 *
 * Contrato: lê o JSON do hook no stdin, escreve achados no stderr e sai com 2
 * para que o Claude veja e corrija. Sem achados, sai 0 em silêncio.
 */

const fs = require("fs");

function lerStdin() {
  try {
    return fs.readFileSync(0, "utf8");
  } catch {
    return "";
  }
}

function caminhoDoArquivo(payload) {
  const entrada = payload && payload.tool_input;
  if (!entrada) return null;
  return entrada.file_path || entrada.path || null;
}

// Placeholders e constantes que não são segredo de verdade.
const VALOR_INOCENTE =
  /^(|\s*|changeme|placeholder|example|exemplo|test|teste|dummy|none|null|todo|xxx+|\*+|\$\{[^}]*\}|bearer |bearer-key|authorization|content-type|application\/json|utf-8)$/i;

const REGRAS = [
  {
    id: "segredo-hardcoded",
    gravidade: "CRÍTICO",
    // Atribuição de literal a algo com cara de credencial.
    regex:
      /\b(?:secret|secretKey|senha|password|passwd|apiKey|api_key|accessKey|secretAccessKey|privateKey|hmacSecret|webhookSecret|clientSecret)\w*\s*=\s*"([^"]*)"/gi,
    aceita: (m) => !VALOR_INOCENTE.test(m[1]) && m[1].length >= 6,
    mensagem:
      "Credencial literal no código. Use variável de ambiente via @Value e deixe o valor fora do repositório.",
  },
  {
    id: "fallback-de-segredo",
    gravidade: "ALTO",
    // ${VAR:valorPadrao} onde a variável é uma credencial — fallback previsível.
    regex: /\$\{([A-Z0-9_]+)\s*:\s*([^}]*)\}/g,
    aceita: (m) => {
      const nome = m[1];
      // Só credencial importa. Duração, URL, host, porta e flag têm valor
      // padrão legitimamente.
      const ehCredencial =
        /(?:SECRET|PASSWORD|PASSWD|SENHA|TOKEN|CREDENTIAL|HMAC|APIKEY|API_KEY|_KEY$|^KEY_)/.test(
          nome
        );
      const naoEhSegredo =
        /(?:EXPIRATION|EXPIRES|TTL|TIMEOUT|_URL$|URI|HOST|PORT|ENABLED|MODE|REGION|BUCKET|VHOST|_NAME$|_USER$|ORIGINS|STRATEGY)/.test(
          nome
        );
      if (!ehCredencial || naoEhSegredo) return false;
      return !VALOR_INOCENTE.test(m[2].trim());
    },
    mensagem:
      "Segredo com valor padrão embutido. Se a variável faltar em produção, a aplicação sobe com um segredo público. Deixe sem fallback e falhe o boot.",
  },
  {
    id: "stacktrace",
    gravidade: "MÉDIO",
    regex: /\.printStackTrace\s*\(/g,
    mensagem:
      "printStackTrace() em vez de log estruturado. Use o logger (log.error) e não devolva o rastro ao cliente.",
  },
  {
    id: "vazamento-de-excecao",
    gravidade: "ALTO",
    // Mensagem de exceção indo para o corpo da resposta.
    regex:
      /\.body\s*\(\s*[^)]*\b(?:ex|e|exception|throwable|t)\s*\.\s*get(?:Localized)?Message\s*\(/gi,
    // Só acusa quando a exceção NÃO é nossa. Mensagem de ValidationException ou
    // BusinessException é texto que nós escrevemos para o usuário final e pode ir
    // na resposta; já a de Exception, RuntimeException ou erro de banco carrega
    // SQL, nome de tabela e caminho de arquivo.
    aceita: (m, texto) => {
      const antes = texto.slice(0, m.index);
      const handler = antes.lastIndexOf("@ExceptionHandler");
      if (handler === -1) return true;
      const tipo = texto.slice(handler, handler + 200);
      return /@ExceptionHandler\s*\(\s*(?:\{\s*)?(?:Exception|Throwable|RuntimeException|SQL\w*|DataAccess\w*|Persistence\w*|Hibernate\w*)\s*\.class/.test(
        tipo
      );
    },
    mensagem:
      "Mensagem de exceção genérica no corpo da resposta vaza detalhe interno (SQL, nome de tabela, caminho). Logue completo no servidor com um id de correlação e devolva mensagem genérica.",
  },
  {
    id: "rota-liberada",
    gravidade: "ALTO",
    regex: /\.permitAll\s*\(\s*\)/g,
    mensagem:
      "Rota pública nova. Confirme que é intencional e que ela tem rate limit — rota pública sem limite é força bruta ou criação em massa de conta.",
  },
  {
    id: "cors-aberto",
    gravidade: "ALTO",
    regex:
      /(?:setAllowedOriginPatterns|setAllowedOrigins|setAllowedHeaders|allowedOrigins|allowedHeaders)\s*\(\s*(?:List\.of\s*\(\s*)?"\*"/g,
    mensagem:
      "CORS liberado com curinga. Use a allowlist de ${CORS_ALLOWED_ORIGINS}; curinga junto com allowCredentials é especialmente grave.",
  },
  {
    id: "crossorigin",
    gravidade: "MÉDIO",
    regex: /@CrossOrigin\b/g,
    mensagem:
      "@CrossOrigin no controller contorna a configuração central de CORS do SecurityConfig. Configure lá.",
  },
  {
    id: "aleatorio-fraco",
    gravidade: "ALTO",
    regex:
      /\b(?:Math\.random\s*\(\s*\)|new\s+Random\s*\()/g,
    mensagem:
      "Aleatoriedade não criptográfica. Para código de convite, token ou chave de arquivo use SecureRandom ou UUID.randomUUID().",
  },
  {
    id: "nome-de-arquivo-do-cliente",
    gravidade: "CRÍTICO",
    regex: /getOriginalFilename\s*\(/g,
    mensagem:
      "getOriginalFilename() vem do cliente e abre path traversal se virar chave de storage ou caminho. Gere a chave no servidor (UUID) e valide magic bytes.",
  },
  {
    id: "comparacao-de-assinatura",
    gravidade: "CRÍTICO",
    // equals() em algo com cara de assinatura — vulnerável a timing attack.
    regex:
      /\b\w*(?:signature|assinatura|hmac|digest|mac)\w*\s*\.\s*equals\s*\(|\.equals\s*\(\s*\w*(?:signature|assinatura|hmac|digest|mac)\w*\s*\)/gi,
    mensagem:
      "Comparação de assinatura com equals() é vulnerável a timing attack. Use MessageDigest.isEqual().",
  },
  {
    id: "query-concatenada",
    gravidade: "CRÍTICO",
    // @Query cuja string é montada com concatenação.
    regex: /@Query\s*\(\s*(?:value\s*=\s*)?"[^"]*"\s*\+/g,
    mensagem:
      "@Query montada por concatenação é injeção de SQL/JPQL. Use parâmetro nomeado (:param) com @Param.",
  },
  {
    id: "chamada-externa-sem-timeout",
    gravidade: "MÉDIO",
    regex: /RestClient\s*\.\s*builder\s*\(/g,
    exigeAusenciaDe: /setReadTimeout|connectTimeout/,
    mensagem:
      "RestClient sem timeout explícito de connect e read. Sem isso um worker trava para sempre. Siga o molde de infra/push/ExpoPushService.",
  },
  {
    id: "ddl-auto-destrutivo",
    gravidade: "CRÍTICO",
    regex: /ddl-auto\s*[:=]\s*(?:create|create-drop|update)/g,
    mensagem:
      "ddl-auto destrutivo. Só é aceitável no perfil de teste; em dev ou prod o schema é do Flyway.",
  },
];

function achadosEm(texto) {
  const linhas = texto.split(/\r?\n/);
  const achados = [];

  for (const regra of REGRAS) {
    if (regra.exigeAusenciaDe && regra.exigeAusenciaDe.test(texto)) continue;

    regra.regex.lastIndex = 0;
    let m;
    while ((m = regra.regex.exec(texto)) !== null) {
      if (regra.aceita && !regra.aceita(m, texto)) continue;

      const linha = texto.slice(0, m.index).split(/\r?\n/).length;
      const conteudo = (linhas[linha - 1] || "").trim();

      // Comentário não é código: não acusa.
      if (/^(\/\/|\*|\/\*|--|#)/.test(conteudo)) continue;

      achados.push({ regra, linha, conteudo });
      if (achados.filter((a) => a.regra.id === regra.id).length >= 3) break;
    }
  }
  return achados;
}

function main() {
  const bruto = lerStdin();
  if (!bruto.trim()) process.exit(0);

  let payload;
  try {
    payload = JSON.parse(bruto);
  } catch {
    process.exit(0); // Payload inesperado nunca bloqueia o trabalho.
  }

  const arquivo = caminhoDoArquivo(payload);
  if (!arquivo) process.exit(0);
  if (!/\.(java|ya?ml)$/i.test(arquivo)) process.exit(0);
  if (/[\\/](target|build)[\\/]/.test(arquivo)) process.exit(0);

  // O próprio scanner e o perfil de teste têm os padrões por definição.
  if (/java-security-scan\.js$/.test(arquivo)) process.exit(0);
  const ehPerfilDeTeste = /application-test\.ya?ml$/i.test(arquivo);

  let texto;
  try {
    texto = fs.readFileSync(arquivo, "utf8");
  } catch {
    process.exit(0);
  }

  let achados = achadosEm(texto);
  if (ehPerfilDeTeste) {
    achados = achados.filter(
      (a) => a.regra.id !== "ddl-auto-destrutivo" && a.regra.id !== "segredo-hardcoded"
    );
  }
  if (achados.length === 0) process.exit(0);

  const ordem = { "CRÍTICO": 0, ALTO: 1, "MÉDIO": 2 };
  achados.sort((a, b) => ordem[a.regra.gravidade] - ordem[b.regra.gravidade]);

  const nome = arquivo.replace(/\\/g, "/").split("/").pop();
  const linhas = [
    `Varredura de segurança em ${nome} — ${achados.length} ponto(s) a verificar:`,
    "",
  ];
  for (const a of achados) {
    linhas.push(`[${a.regra.gravidade}] ${arquivo}:${a.linha}`);
    linhas.push(`  ${a.conteudo}`);
    linhas.push(`  ${a.regra.mensagem}`);
    linhas.push("");
  }
  linhas.push(
    "Corrija o que for procedente. Se algum achado for falso positivo neste contexto, diga por quê e siga."
  );

  process.stderr.write(linhas.join("\n") + "\n");
  process.exit(2);
}

main();
