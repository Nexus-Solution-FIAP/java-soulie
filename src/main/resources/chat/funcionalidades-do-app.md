# Guia do aplicativo para a Alma

Revisão: 23/10/2026. Situação: recursos encontrados no app mobile e na API locais. A presença no código não certifica disponibilidade em produção. Não descrever demonstrações antigas como se fossem a versão instalada pelo usuário.

## 1. Onde encontrar cada função

| Tela ou aba | O que orientar |
| --- | --- |
| Login / Cadastro | Entrada com e-mail e senha; criação da conta e escolha da planta inicial |
| Meu Soulie | Ver a planta e seu XP, regar, personalizar nome e selecionar espécie desbloqueada |
| Dashboard | Consultar missões por período, ler requisitos, realizar missão e pedir ajuda à Alma |
| Jardim | Criar ou entrar em jardim por código, consultar ranking e feed e interagir |
| Soulie IA | Conversar com Alma; esse é o rótulo do chat na navegação revisada |
| Perfil | Ver estatísticas, plantas conquistadas, conquistas, atividade recente e configurações |
| Preferências do App | Ajustar tema, som, vibração e tela inicial |
| Privacidade e Dados | Controlar cutucadas e nome público, preferência de resumo semanal, baixar dados e solicitar exclusão |

As missões ficam no **Dashboard**. Evite inventar uma aba "Missões" ou botão "Enviar Comprovação" quando esses não forem os rótulos da versão revisada. Para validação, use [sistema-missoes.md](sistema-missoes.md).

Fontes: F01 e F08, no [mapa de fontes](README.md#fontes-locais).

## 2. Cadastro e primeiros passos

O cadastro pede nome, e-mail, senha e confirmação. A regra exibida é: pelo menos 8 caracteres, incluindo maiúscula, minúscula, número e um símbolo entre `@$!%*?&`. Oriente a verificar a regra localmente; nunca peça a senha no chat.

No cadastro, escolhe-se uma planta elegível da flora brasileira. Isso não libera toda a coleção. Depois de entrar, uma orientação inicial útil é conhecer Meu Soulie e escolher no Dashboard uma missão que caiba na rotina.

Se houver erro, pergunte se aparece ao cadastrar ou entrar e qual é o aviso. Não atribua automaticamente a falha à senha, não confirme existência de conta de terceiros e não invente um fluxo de redefinição de senha.

Fonte: F01.

## 3. Cuidado e personalização

Em **Meu Soulie**, é possível regar, editar o nome e escolher uma espécie disponível pela personalização. **Sobre a planta** apresenta informações da espécie. Toques e animações são interações visuais; não anuncie que concedem XP.

Regar registra cuidado e ajuda na sequência. Crescimento e desbloqueios seguem [missões e evolução](sistema-missoes.md). Não confunda regar a planta virtual com uma recomendação de rega diária para plantas reais: a necessidade de água depende das condições de cultivo.

Fontes: F01, F04 e F06.

## 4. Coleção de plantas

Cada espécie tem XP próprio. Ao trocar, a nova espécie usa seu progresso salvo; a anterior conserva o dela. Recompensas obtidas com a espécie ativa contribuem para o XP dela. Não diga que uma troca copia todo o XP entre plantas.

Há 17 espécies na versão revisada. Dez podem ser escolhidas como inicial. A escolhida inicialmente fica liberada sem precisar cumprir a condição de conquista; a tabela mostra como obter as demais que ainda estiverem bloqueadas.

| Espécie | Pode ser inicial? | Condição fora da escolha inicial |
| --- | --- | --- |
| Ipê Amarelo | Sim | Concluir 5 missões |
| Guaimbê-ondulado | Sim | Concluir 8 missões |
| Manacá-da-serra | Sim | Desbloquear 3 conquistas |
| Maranta | Sim | Concluir 12 missões |
| Pacová | Sim | Desbloquear 5 conquistas |
| Pitangueira | Sim | Levar 1 planta ao nível máximo |
| Rainha-do-abismo | Sim | Manter sequência de 7 dias |
| Trevo roxo | Sim | Concluir 20 missões |
| Mulungu | Sim | Desbloquear 7 conquistas |
| Sucupira-preta | Sim | Levar 2 plantas ao nível máximo |
| Girassol | Não | Concluir 15 missões |
| Rosa | Não | Resgatar código válido |
| Hibisco | Não | Desbloquear 10 conquistas |
| Lavanda | Não | Levar 1 planta ao nível máximo |
| Margarida | Não | Concluir 30 missões |
| Orquídea | Não | Resgatar código válido |
| Árvore Tecnológica | Não | Resgatar código válido |

Essas condições usam o histórico da conta: missões concluídas, conquistas desbloqueadas, plantas que atingiram 200 XP ou sequência atual, conforme o tipo. Não confunda Chama Constante, de 3 dias, com a condição de 7 dias da Rainha-do-abismo.

O catálogo recebido informa liberação e dica de desbloqueio. Para códigos, oriente usar **Código secreto** na personalização com um código obtido por canais do projeto. Esta base não distribui códigos encontrados no código-fonte nem inventa novos códigos.

**Árvore Tecnológica** é uma criação digital do Soulie, não uma espécie botânica real. Para outras plantas, explique primeiro sua função no jogo. Nomes científicos, origem, habitat e cuidados reais exigem fonte botânica própria; o texto do app não certifica essas informações. Não recomende ingestão ou uso medicinal de espécies da coleção.

Fonte: F06.

## 5. Jardim, ranking e feed

Na aba **Jardim**, é possível criar um jardim ou entrar com código de convite. Feed e ranking são relativos ao jardim atual. Entrar em outro faz a pessoa sair do anterior no fluxo mobile; avise dessa consequência ao orientar a troca. Não prometa participação simultânea em vários jardins nesse fluxo.

O ranking do jardim compara a pontuação dos participantes ativos. O perfil também tem posição global, com alcance diferente. Especifique de qual ranking fala e não misture os números. Não há base para anunciar reinício semanal ou mensal do ranking desta versão.

No feed, usuários podem publicar texto, curtir, comentar e apagar a própria publicação. Publicar uma ação não substitui a comprovação da missão nem concede XP automaticamente. Não diga que é possível apagar publicação alheia ou que Alma já publicou pela pessoa.

**Cutucar** envia um lembrete para continuar as atividades. A API limita a três cutucadas por destinatário por dia, exige compartilhar jardim ou seguir um ao outro e respeita a preferência do destinatário. O aviso no aparelho depende também das permissões e da configuração de notificações.

Cutucar não rega a planta alheia, não transfere XP e não implementa o bônus de rega solidária da proposta antiga. Primeiro lugar sozinho não basta para Líder do Jardim: é necessário ter ao menos dois participantes ativos na disputa.

Fontes: F05 e F07. Estimativas de impacto: F11.

## 6. Perfil, preferências e notificações

**Perfil** reúne estatísticas, conquistas, coleção e atividade recente. O XP da planta e o total da conta podem diferir pelo progresso por espécie. Sem dados pessoais, Alma explica onde consultar.

Em **Preferências do App**, há tema Claro, Escuro ou Sistema, som, vibração e tela inicial. O próprio texto da tela ressalva que partes da interface ainda usam o visual claro; não prometa alteração completa de todas as telas.

O controle de **Notificações** no perfil e a permissão do sistema operacional influenciam os avisos. Som ou vibração desligados não significam necessariamente notificações desativadas. Não prometa push em qualquer aparelho ou envio de e-mail apenas por existir uma preferência.

Fonte: F08.

## 7. Privacidade e dados

Em **Privacidade e Dados**, há controles para permitir cutucadas, mostrar nome completo no ranking/feed ou usar apelido e registrar preferência de resumo semanal por e-mail. A opção de resumo não confirma que a rotina de envio esteja operante.

**Baixar meus dados** prepara um JSON com perfil, atividades e conquistas no contrato revisado. Não anuncie que inclui todas as fotos originais, todo o banco ou histórico do chat. A exportação é feita pelo usuário no app, não pela Alma.

**Excluir conta** pede confirmação no app. No código revisado, o perfil é desativado, o token de notificações é limpo e a pessoa sai do jardim. Isso não comprova eliminação física imediata de todos os registros e imagens. Não invente prazo de retenção.

As fotos entram no fluxo de validação. Evite rostos de terceiros, documentos e detalhes privados desnecessários na evidência. Não diga que as imagens são públicas ou visíveis a todo o jardim. Também não prometa criptografia, anonimização ou conformidade jurídica integral apenas com base na intenção do projeto.

Fonte: F08. Veja a diferença entre intenção e implementação em [visao-do-projeto.md](visao-do-projeto.md).

## 8. Solução de problemas

| Relato | Orientação inicial |
| --- | --- |
| Câmera não abre | Conferir permissão nos ajustes do aparelho; usar galeria se disponível. |
| Envio deu erro | Perguntar aviso e etapa; conferir conexão e estado da missão antes de reenviar. |
| Foto recusada | Pedir motivo e descrição; orientar evidência adequada sem garantir aprovação. |
| Análise demorou | Distinguir pendência de falha; conferir retorno disponível sem prometer prazo. |
| XP não apareceu | Verificar aprovação e atualizar dados; lembrar a conclusão única por período. |
| Planta menor após troca | Explicar XP próprio por espécie; não afirmar perda sem dados. |
| Código não funciona | Conferir aviso e código com a origem; diferenciar código de jardim de código de planta. |
| Não recebi aviso | Conferir cutucadas, notificações e permissões do aparelho. |
| Tela diferente do guia | Pedir tela e texto do aviso; considerar diferença de versão. |

Se persistir, peça o erro e uma descrição breve, sem senha ou token. Indique somente o canal de suporte informado na versão do app ou pelo responsável pelo projeto. Não há contato ou prazo de atendimento confirmado nesta base.

Fontes: F01, F03 e F08.
