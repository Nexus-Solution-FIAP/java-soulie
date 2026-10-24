ALTER TABLE daily_missions
    ADD COLUMN period VARCHAR(16) NOT NULL DEFAULT 'DAILY';

INSERT INTO daily_missions (emoji, name, tag, description, xp, co2_saved_kg, water_saved_liters, period)
VALUES ('♻️', 'Reciclagem da Semana', 'Reciclagem', 'Separe o reciclável em pelo menos 3 dias desta semana', 80, 1.05, 0, 'WEEKLY'),
       ('🚲', 'Semana Sem Carro', 'Mobilidade', 'Faça 3 deslocamentos só de bike, a pé ou transporte público', 100, 4.80, 0, 'WEEKLY'),
       ('💧', 'Meta Semanal de Água', 'Água', 'Tome 3 banhos de até 5 minutos nesta semana', 70, 0.36, 180, 'WEEKLY'),
       ('🥦', 'Semana Leve', 'Alimentação', 'Faça 3 refeições sem carne nesta semana', 90, 4.50, 0, 'WEEKLY'),
       ('🌻', 'Horta do Mês', 'Natureza', 'Plante ou cuide de uma horta ou muda neste mês', 150, 2.00, 0, 'MONTHLY'),
       ('🛍️', 'Mês Zero Plástico', 'Consumo', 'Passe um dia inteiro sem descartáveis e registre', 120, 0.45, 0, 'MONTHLY'),
       ('⚡', 'Auditoria de Energia', 'Energia', 'Revise e desligue aparelhos em standby da casa', 100, 0.50, 0, 'MONTHLY'),
       ('🗑️', 'Limpeza Comunitária', 'Resíduos', 'Recolha resíduos em um espaço público ou condomínio', 180, 1.50, 0, 'MONTHLY'),
       ('🌳', 'Árvore do Ano', 'Natureza', 'Plante uma árvore ou adote uma muda permanente', 400, 8.00, 0, 'YEARLY'),
       ('🌍', 'Mutirão Anual', 'Resíduos', 'Participe de uma ação coletiva de coleta ou reciclagem', 300, 3.00, 0, 'YEARLY'),
       ('🏡', 'Hábito Sustentável', 'Consumo', 'Substitua um item descartável da rotina por um reutilizável', 250, 1.20, 0, 'YEARLY');
