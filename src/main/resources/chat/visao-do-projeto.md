# Visão do projeto e situação das funcionalidades

Revisão: 23/10/2026. Uso: conhecimento para perguntas sobre o projeto, propostas e diferenças entre documentos. Cada afirmação de futuro deve conservar sua indicação de situação ao ser citada.

## 1. Propósito

SoulUp é a plataforma; Soulie é o aplicativo e a planta virtual; Alma é a assistente que orienta o usuário. O projeto acadêmico da Nexus Solutions, na FIAP, usa a metáfora de cuidado com uma planta para apoiar hábitos sustentáveis, aprendizado e participação comunitária.

A proposta busca transformar intenção em ações possíveis no cotidiano. Trata-se de propósito do produto, não de comprovação de aumento de engajamento, redução de emissões ou melhora de saúde dos usuários.

As decisões atuais de identidade estão registradas no [README](README.md#decisões-e-limites-da-entrega). Contexto histórico: F10.

## 2. Como interpretar a situação de um recurso

- **Verificado no código local:** encontrado na versão de app/API analisada. Não certifica implantação nem configuração de serviços externos.
- **Visão conceitual documentada:** proposta presente nos materiais do projeto, com disponibilidade atual não confirmada.
- **Não confirmado:** faltam evidências suficientes para afirmar funcionamento, regra, prazo ou política.

A documentação conceitual descreve uma intenção. Para ensinar o funcionamento atual, use [missões e evolução](sistema-missoes.md) e o [guia do app](funcionalidades-do-app.md). Não some regras antigas e novas como se coexistissem.

## 3. Quadro de referência

| Tema | O que foi verificado no código local | Visão conceitual documentada ou limite |
| --- | --- | --- |
| Evolução | XP por espécie; Semente, Broto, Planta Jovem e Árvore; estágio máximo a partir de 200 XP | A proposta histórica usa Semente, Muda, Árvore e Flores/Frutos com dias estimados. Não é a regra atual de evolução nem promessa de calendário. |
| Inatividade | Sequência diária pode ser interrompida e reiniciada | Regressão parcial após 7 dias consta da visão conceitual; sua aplicação atual não foi confirmada. Não ameaçar perda de estágio. |
| Regar | Cuidado diário que registra atividade, sem XP direto | A proposta de "rega comportamental" associa cuidado a ações sustentáveis. Não elimina o botão de rega encontrado no app. |
| Cooperação | Jardim, feed, ranking e cutucadas | Regar a planta de um amigo e receber 5% dos pontos é visão conceitual, sem funcionamento confirmado no fluxo mobile. Cutucada não implementa essa mecânica. |
| Diversidade de ações | Missões têm requisitos, XP de catálogo e uma conclusão por período | Bônus de diversidade de 100% e redução sucessiva de 30% por repetição aparecem no documento conceitual. Não aplicar esses cálculos às recompensas atuais. |
| Validação de foto | Pipeline que recebe a imagem, analisa e condiciona a recompensa à aprovação | A documentação antiga divide validação heurística e visão computacional em fases. Serviços e modos de demonstração dependem de configuração; não chamar mock de análise real. |
| Assistente | Chat mobile e endpoint com respostas fixas por palavras-chave | Integração IBM Watson é citada na proposta histórica. Plataforma generativa e conexão desta base ainda serão definidas; não anunciar Watson em operação. |
| Recompensas e parcerias | Há XP e conquistas no fluxo mobile revisado | Marketplace, conteúdo premium, campanhas de marcas e expansão institucional são visão de produto. Não há compra, valor, parceiro ativo ou data confirmados nesta base. |
| Privacidade | Preferências, exportação delimitada e desativação de conta no fluxo revisado | Retenção limitada, eliminação ampla, consentimentos e outras medidas aparecem como princípios da proposta; não são garantia de implementação completa. |
| Prevenção de fraude | Há regras técnicas no envio e processamento da evidência | Hash de duplicidade, leitura de metadados, moderação humana e detecção de anomalias são citados na proposta. Não afirmar que todos estão ativos ou que qualquer fraude é detectada. |

Fontes: F02 a F11, no [README](README.md#fontes-locais). O quadro não é um compromisso de desenvolver todas as propostas.

## 4. Como falar de futuro

Quando perguntarem sobre uma proposta, explique a ideia e a situação na mesma resposta:

> "A rega solidária aparece na visão do projeto como uma forma de apoiar plantas de amigos. Não tenho confirmação de que essa função esteja disponível na versão atual. O recurso verificado no Jardim é a cutucada, que envia um lembrete."

Não use "em breve", "na próxima atualização" ou datas de lançamento sem confirmação atual do responsável. Horizontes de MVP, V1 e V2 do PDF são planejamento histórico, não um cronograma vigente.

Se perguntarem pela regressão:

> "O documento conceitual propõe regressão após sete dias sem atividade, mas isso não foi confirmado no fluxo atual revisado. A regra verificada é a da sequência diária: ela pode reiniciar sem apagar automaticamente o XP acumulado."

Se perguntarem sobre IA ou limites de imagem, explique que um serviço pode analisar o que a foto mostra sem conseguir comprovar sozinho duração de banho, frequência semanal ou todo o trajeto. Não apresente uma hipótese de implementação como capacidade demonstrada.

## 5. O que não deve virar promessa

Não divulgar como resultado real os números ilustrativos de protótipos, metas de mercado, KPIs, quantidade de usuários, impacto ambiental acumulado ou comparações de desempenho. Não prometer certificação ESG, créditos de carbono, prêmios financeiros, parceria institucional, proteção jurídica integral ou prazo de suporte.

A base mantém a visão completa do projeto com transparência sobre evidências. A disponibilidade para um usuário depende da versão, dos serviços conectados e das informações recebidas pela aplicação.
