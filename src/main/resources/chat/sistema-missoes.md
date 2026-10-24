# Missões, XP, sequência e conquistas

Revisão: 23/10/2026. Situação: comportamento verificado no código local do contrato mobile. Catálogos e valores abaixo são referências da versão revisada; o catálogo recebido pela aplicação informa o que está disponível para cada usuário.

## 1. Como realizar uma missão

1. Abra **Dashboard** e escolha o período das missões.
2. Toque na missão e leia a descrição e o XP exibido.
3. Realize a ação de verdade. Se precisar de orientação, o botão **?** encaminha a missão para Alma.
4. Toque em **Realizar Missão**. Fotografe a evidência ou escolha uma foto da sua ação na galeria.
5. Revise a foto e envie pelo fluxo de comprovação.
6. Confira o resultado da análise. O envio, por si só, não conclui a missão nem garante recompensa.

No catálogo mobile atual, a foto é obrigatória. Não oriente a concluir missões apenas por conversa, publicação no feed, texto de atividade, quiz ou check-in. Uma sugestão de hábito feita pela Alma também não cria uma missão no catálogo.

Fontes: F01, F03 e F09, no [README](README.md#fontes-locais).

## 2. Períodos e repetição

| Período | Janela da regra atual | Quando pode gerar uma nova conclusão |
| --- | --- | --- |
| Diária | Dia de referência da API | Em outro dia |
| Semanal | Segunda-feira a domingo | Na semana seguinte |
| Mensal | Mês do calendário | No mês seguinte |
| Anual | Ano do calendário | No ano seguinte |

A mesma missão concede a recompensa de conclusão uma vez por janela. Reenviar no mesmo período não é uma forma de acumular XP. Quando a janela muda, a missão pode voltar a ficar aberta; isso não significa que haverá uma tarefa diferente ou um catálogo novo.

Não confunda "três deslocamentos na semana", requisito de uma missão, com "concluir essa missão três vezes na semana". Siga a descrição. Se uma ação de vários dias não puder ser comprovada claramente pela foto, reconheça a limitação da evidência e peça a instrução exibida; não invente um método garantido de aprovação.

O código usa a data da API para as janelas. Esta revisão não confirmou um fuso de produção contratual; evite prometer renovação à meia-noite do celular. Em divergência, peça a data de conclusão e o aviso exibido.

Fonte: F03.

## 3. Foto e resultado da análise

| Situação recebida | Significado | Orientação da Alma |
| --- | --- | --- |
| `PENDING` | A análise não terminou; sem XP de conclusão naquele momento | Não tratar como rejeição. Conferir o retorno do app e notificações disponíveis, sem reenviar repetidamente ou fixar prazo. |
| `APPROVED` | Evidência aprovada; o sistema aplica a recompensa conforme o período | Usar os dados efetivamente retornados. Se houver divergência de saldo, orientar atualizar os dados e informar o aviso. |
| `REJECTED` | A evidência não comprovou a missão na análise | Explicar o motivo recebido e orientar nova foto compatível com a descrição quando permitido, sem acusar fraude. |
| `FAILED` | Houve falha técnica na análise | Distinguir de rejeição. Conferir o aviso e tentar novamente quando o app permitir, sem prometer reprocessamento ou aprovação. |

Permissão negada, falha ao abrir câmera, envio interrompido e erro na análise são etapas diferentes. Erro de conexão não prova que a foto estava ruim. Tempo esgotado não comprova se o servidor recebeu ou não a evidência; confira o estado antes de recomendar outro envio. Não invente uma tela de acompanhamento se ela não estiver disponível na versão do usuário.

Para melhorar a evidência: boa iluminação, imagem nítida e foco no que a missão pede. Evite objetos cobrindo a ação e detalhes pessoais desnecessários. Use foto da ação real; não recomende imagem da internet, montagem ou foto gerada. Não peça imagens de pessoas no banho ou em outra situação íntima para comprovar economia de água.

Alma não é o serviço que analisa fotos. Sem resultado do sistema, não descreva objetos detectados, porcentagem de confiança, duplicidade ou motivo da reprovação como se tivesse examinado a imagem. Uma bicicleta visível não demonstra sozinha quantos trajetos foram feitos.

Fonte: F03. Os documentos técnicos F10 explicam a intenção do pipeline, mas os detalhes de espera devem ser conferidos no controller atual, conforme o README.

## 4. XP da conta e XP da planta

Missões aprovadas adicionam pontos à conta e XP à planta ativa. Conquistas desbloqueadas podem conceder XP adicional, conforme o catálogo. Não prometa um bônus universal para toda foto, sequência ou convite.

O **total da conta**, usado nas estatísticas e classificações, e o **XP de cada espécie**, usado no estágio da planta, são informações distintas. Cada espécie guarda seu progresso. Trocar para uma espécie com menos XP pode mostrar um estágio menor sem apagar o progresso da anterior.

Não converta XP em dinheiro ou EcoMoedas. Não apresente o total da conta como XP da planta selecionada sem verificar o campo recebido.

### Estágios visuais

| XP acumulado da espécie | Nível | Estágio |
| --- | --- | --- |
| 0 a 49 | 0 | Semente |
| 50 a 99 | 1 | Broto |
| 100 a 199 | 2 | Planta Jovem |
| 200 ou mais | 3 | Árvore |

A evolução é por XP, sem quantidade fixa de dias. Com 80 XP na espécie faltam 20 para Planta Jovem. Com 200 XP ela já está no estágio visual máximo desta versão. Não anuncie um estágio adicional aos 500 XP: há divergência técnica no campo de próxima meta no nível máximo, mas ambos os cálculos de nível param em Árvore.

Os estágios são rótulos do jogo. Uma espécie herbácea chegar a "Árvore" não significa que ela se torna uma árvore na natureza.

Fontes: F02, F03, F05 e F06.

## 5. Rega e sequência diária

- **Regar**, em Meu Soulie, registra o cuidado do dia e não concede XP diretamente.
- Regar ou concluir uma missão aprovada registra um dia de atividade. Fazer as duas coisas no mesmo dia não soma dois dias à sequência.
- Uma rega já registrada no dia não precisa ser repetida para contar novamente.
- Atividade em dias consecutivos mantém a sequência. Ao deixar um dia inteiro sem atividade válida, ela pode voltar a zero; a próxima atividade inicia uma sequência de um dia.
- Uma conquista de sequência pode ser desbloqueada pela rega e conceder sua recompensa própria. Isso não transforma toda rega em uma ação com XP.
- A sequência é da conta, não uma sequência independente para cada espécie.

Perder a sequência não implica apagar XP, plantas ou conquistas. Não foi identificado um mecanismo automático de regressão por sete dias sem uso no fluxo revisado. Essa ideia pertence à [visão conceitual](visao-do-projeto.md).

Fontes: F04, F05 e F06.

## 6. Conquistas verificadas

As condições combinam o catálogo inicial com a atualização da migration V13 e o cálculo atual do serviço. Não use apenas as descrições antigas da migration V12.

| Conquista | Condição da versão revisada | XP próprio da conquista no catálogo |
| --- | --- | --- |
| Reciclador Consciente | Concluir 3 missões de Reciclagem | 50 |
| Ciclista Eco | Concluir 3 missões de Mobilidade | 40 |
| Guardião das Águas | Acumular 120 litros estimados de água economizada nas missões | 35 |
| Chama Constante | Manter sequência de 3 dias | 60 |
| Líder do Jardim | Ficar em 1º lugar num jardim com ao menos 2 participantes ativos | 75 |
| Compostador Mestre | Concluir 3 missões de Resíduos | 45 |
| Zero Plástico | Concluir 3 missões de Consumo | 50 |
| Transporte Coletivo | Concluir 8 missões de Mobilidade | 40 |
| Economia Solar | Concluir 3 missões de Energia | 55 |
| Árvore Majestosa | Alcançar o nível 3 da planta | 100 |

Os nomes são temáticos: Ciclista Eco e Transporte Coletivo contam Mobilidade; não exigem exclusivamente bicicleta ou exclusivamente ônibus. A recompensa é concedida quando a conquista é desbloqueada, não a cada consulta ou repetição do marco. A base não sabe quais o usuário já tem.

Consulte **Perfil** para conquistas e progresso. Se o catálogo atual recebido trouxer outro requisito ou recompensa, use essa informação e registre a divergência para revisão.

Fonte: F05.

## 7. Exemplos de missões e impacto

O catálogo revisado inclui reciclagem, mobilidade, água, consumo, energia, resíduos, alimentação e natureza. Exemplos de cadastro são Reciclar Resíduos, Transporte Público, Economia de Água, Compostagem e Plantar uma Muda. Eles não provam que essas missões estão abertas para a pessoa agora.

Economia de Água tem 25 XP no cadastro revisado. Isso não é uma promessa de XP por qualquer banho curto: depende da missão disponível, da janela e da aprovação. Uma dica de usar garrafa reutilizável também não vale XP automaticamente.

Água e CO₂ no app são estimativas de referência, não leituras de medidor, auditoria pessoal ou créditos de carbono. Há diferenças entre valores antigos do catálogo e fatores do impacto coletivo; reproduza apenas o valor informado para o contexto correto e identifique-o como estimativa.

Fontes: F03 e F11.
