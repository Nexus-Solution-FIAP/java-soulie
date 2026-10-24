-- A entidade Validation grava o resultado como texto (@Enumerated(EnumType.STRING)),
-- mas a V7 criou `result_validation` como INTEGER. Sem esta conversão a inserção
-- falha em tempo de execução.
--
-- Equivale à V13 do repositório soulie-soul-up, com duas diferenças deliberadas:
--
-- 1. A grafia é corrigida. Lá o enum era `APROVED` (sem o segundo P) e `REJECT`,
--    e a migration gravava esses literais no banco. Aqui passa a ser `APPROVED`
--    e `REJECTED`. Corrigir agora é barato — ainda não há dado em produção
--    dependendo da grafia errada.
-- 2. Entra `FAILED`, para o caso em que a IA externa não conseguiu responder
--    (timeout, erro, mensagem esgotada na DLQ). Sem esse estado, uma falha de
--    infraestrutura ficaria indistinguível de uma foto legitimamente reprovada.

ALTER TABLE validations
    ALTER COLUMN result_validation TYPE VARCHAR(20) USING
        CASE result_validation
            WHEN 1 THEN 'APPROVED'
            WHEN 2 THEN 'REJECTED'
            ELSE 'PENDING'
        END;

ALTER TABLE validations
    ALTER COLUMN result_validation SET DEFAULT 'PENDING';

ALTER TABLE validations
    ADD CONSTRAINT check_validations_result
        CHECK (result_validation IN ('PENDING', 'APPROVED', 'REJECTED', 'FAILED'));

-- O ApprovedEvidenceValidator consulta por evidência e por resultado.
CREATE INDEX idx_validations_result ON validations (result_validation);
