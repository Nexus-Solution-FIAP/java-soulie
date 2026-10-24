-- Conquistas passam a seguir metas claras: tipo de missão, sequência, água, nível da planta e 1º lugar.

UPDATE achievements
SET description = 'Complete 3 missões de reciclagem',
    target      = 3
WHERE name = 'Reciclador Consciente';

UPDATE achievements
SET description = 'Complete 3 missões de mobilidade',
    target      = 3
WHERE name = 'Ciclista Eco';

UPDATE achievements
SET description = 'Economize 120 litros de água nas missões',
    target      = 120
WHERE name = 'Guardião das Águas';

UPDATE achievements
SET description = 'Mantenha uma sequência de 3 dias seguidos',
    target      = 3
WHERE name = 'Chama Constante';

UPDATE achievements
SET description = 'Fique em 1º lugar no jardim',
    target      = 1
WHERE name = 'Líder do Jardim';

UPDATE achievements
SET description = 'Complete 3 missões de resíduos',
    target      = 3
WHERE name = 'Compostador Mestre';

UPDATE achievements
SET description = 'Complete 3 missões de consumo consciente',
    target      = 3
WHERE name = 'Zero Plástico';

UPDATE achievements
SET description = 'Complete 8 missões de mobilidade',
    target      = 8
WHERE name = 'Transporte Coletivo';

UPDATE achievements
SET description = 'Complete 3 missões de energia',
    target      = 3
WHERE name = 'Economia Solar';

UPDATE achievements
SET description = 'Alcance o nível máximo da sua planta',
    target      = 3
WHERE name = 'Árvore Majestosa';

-- O top 3 liberava a conquista para todo mundo. Mantém só quem já é o 1º de uma disputa.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY points DESC, id ASC) AS pos
    FROM users
    WHERE active IS TRUE
),
leader AS (
    SELECT id FROM achievements WHERE name = 'Líder do Jardim'
),
garden_size AS (
    SELECT COUNT(*) AS total FROM users WHERE active IS TRUE
)
UPDATE user_achievements ua
SET unlocked    = ranked.pos = 1 AND garden_size.total >= 2,
    unlocked_at = CASE
                      WHEN ranked.pos = 1 AND garden_size.total >= 2 THEN COALESCE(ua.unlocked_at, NOW())
                      ELSE NULL
                  END,
    progress    = CASE WHEN garden_size.total >= 2 THEN ranked.pos ELSE 0 END
FROM ranked, leader, garden_size
WHERE ua.user_id = ranked.id
  AND ua.achievement_id = leader.id;
