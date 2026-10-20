---
name: resolve
description: Corrige imediatamente um erro, log ou trecho de codigo quebrado neste projeto Spring Boot. Use quando o usuario digitar /resolve seguido de um erro, log ou codigo que nao funciona.
---

# Skill: /resolve

**Gatilho:** o usuário digita `/resolve` seguido de um erro, log ou trecho que não funciona.

**Ação:** assuma o controle e conserte. Leia os arquivos relevantes antes de responder e, se o erro estiver em arquivo do projeto, aplique a correção direto. Siga este formato exato:

1. **A Solução:** o código corrigido ou o comando, já adaptado a este projeto.
2. **O Erro:** em no máximo 2 frases, o que causava a falha.
3. **Ponto de Atenção:** 1 dica curta para não repetir.

**Antes de chutar, confira as pegadinhas conhecidas** (estão todas no `CLAUDE.md`):

- o starter web do Boot 4 é `spring-boot-starter-webmvc`, e os de teste são divididos (`-data-jpa-test`, `-webmvc-test`, ...);
- as anotações de teste mudaram de pacote (`org.springframework.boot.data.jpa.test.autoconfigure.*`);
- migration vai em `db.migrations` (com ponto) — na pasta errada, é ignorada em silêncio;
- `level` e `date` são palavras reservadas no H2, daí o `globally_quoted_identifiers` no perfil de teste;
- exceção lançada em filtro **não** passa pelo `ErrorHandler`, então vira 500 em vez do status certo;
- entidade com campo `NOT NULL` novo quebra os helpers de teste;
- não há Docker nesta máquina: Testcontainers não roda.

**Restrição:** sem sermão. Rápido, cirúrgico, resolve e explica.
