-- Reconecta a tabela `missions` (criada na V5 e desde então órfã, sem entidade JPA)
-- ao modelo de missão do repositório soulie-soul-up, que agora foi portado.
--
-- Equivale à V12 daquele repositório, renumerada para caber nesta sequência.
-- Atenção: o `avatars.experience` que a V12 de lá também criava NÃO entra aqui.
-- Este projeto já tem `avatars.xp` (XP total, com o nível derivado por
-- SoulieLevel) desde a V12 local, e é esse o modelo que vale.
--
-- Esta tabela `missions` é o modelo LEGADO (missão criada pelo usuário a partir
-- de uma Action, com status e evidência). Não confundir com `daily_missions`,
-- que é o catálogo consumido pelo contrato /v1 do app.

ALTER TABLE missions
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Valores aceitos, espelhando o enum MissionStatus. O CHECK existe para o banco
-- recusar um estado inválido mesmo se alguém escrever direto nele.
ALTER TABLE missions
    ADD CONSTRAINT check_missions_status
        CHECK (status IN ('ACTIVE', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED'));

-- As duas consultas quentes do MissionService: missões de um avatar e
-- missões por estado.
CREATE INDEX idx_missions_avatar_id ON missions (avatar_id);
CREATE INDEX idx_missions_action_id ON missions (action_id);
