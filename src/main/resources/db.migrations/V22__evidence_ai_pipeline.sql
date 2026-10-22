-- Pipeline de validação de imagem por IA.
--
-- Uma só tabela de evidência e uma só de validação servem os DOIS modelos de
-- missão que convivem no projeto:
--   * `missions`       — modelo legado (Action + status), vindo do soulie-soul-up
--   * `daily_missions` — catálogo consumido pelo contrato /v1 do app
--
-- Assim o worker grava APPROVED num lugar só, e isso ao mesmo tempo destrava o
-- `PUT /mission/{id}/complete` legado e credita a recompensa do /v1.

-- ---------------------------------------------------------------------------
-- evidences
-- ---------------------------------------------------------------------------

-- O vínculo com o modelo legado passa a ser opcional: uma evidência do /v1
-- aponta para daily_missions, não para missions.
ALTER TABLE evidences
    ALTER COLUMN mission_id DROP NOT NULL;

ALTER TABLE evidences
    ADD COLUMN daily_mission_id BIGINT,
    ADD COLUMN image_url        VARCHAR(500),
    ADD COLUMN image_key        VARCHAR(255),
    ADD COLUMN content_type     VARCHAR(100),
    ADD COLUMN size_bytes       BIGINT,
    ADD COLUMN submitted_at     TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE evidences
    ADD CONSTRAINT fk_evidences_daily_mission_id
        FOREIGN KEY (daily_mission_id) REFERENCES daily_missions (id);

-- Uma evidência pertence a UM dos dois modelos, nunca aos dois e nunca a
-- nenhum. É este CHECK que mantém o modelo unificado honesto — sem ele, o
-- `exatamente um` vira convenção que alguém quebra sem perceber.
ALTER TABLE evidences
    ADD CONSTRAINT check_evidences_single_mission
        CHECK ((mission_id IS NULL) <> (daily_mission_id IS NULL));

-- A chave do objeto no bucket é gerada pelo servidor (UUID) e é única: se
-- aparecer repetida, houve bug de geração, e é melhor falhar que sobrescrever
-- a foto de outra pessoa.
CREATE UNIQUE INDEX uq_evidences_image_key ON evidences (image_key)
    WHERE image_key IS NOT NULL;

CREATE INDEX idx_evidences_user_id ON evidences (user_id);
CREATE INDEX idx_evidences_daily_mission_id ON evidences (daily_mission_id);

-- ---------------------------------------------------------------------------
-- validations
-- ---------------------------------------------------------------------------
ALTER TABLE validations
    ADD COLUMN detections   VARCHAR(2000),
    ADD COLUMN confidence   DOUBLE PRECISION,
    ADD COLUMN reason       VARCHAR(500),
    ADD COLUMN provider     VARCHAR(100),
    ADD COLUMN requested_at TIMESTAMP,
    ADD COLUMN resolved_at  TIMESTAMP,
    ADD COLUMN attempts     INTEGER NOT NULL DEFAULT 0;

ALTER TABLE validations
    ADD CONSTRAINT check_validations_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1));

-- `evidence_id` já é UNIQUE desde a V7: uma evidência tem no máximo uma
-- validação. É isso que torna a aprovação idempotente — a reentrega de uma
-- mensagem do RabbitMQ não cria uma segunda validação para a mesma evidência.
