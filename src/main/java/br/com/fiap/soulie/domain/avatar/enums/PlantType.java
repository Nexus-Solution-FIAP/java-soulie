package br.com.fiap.soulie.domain.avatar.enums;

import lombok.Getter;

@Getter
public enum PlantType {
    IPE_AMARELO(1, "Ipê Amarelo", "O nome vem do tupi 'ypê', que significa 'casca grossa'. Árvore da Mata Atlântica e do Cerrado, é a flor nacional do Brasil e floresce no auge da seca, quando perde todas as folhas. Encontrada do Sudeste ao Sul e no Centro-Oeste."),
    GUAIMBE_ONDULADO(2, "Guaimbê-ondulado", "Guaimbê vem do tupi e nomeia as plantas que se agarram nos troncos. Vive como epífita na Mata Atlântica, subindo pelas árvores em busca de luz, com folhas de bordas onduladas. Típica do Sul e do Sudeste."),
    MANACA_DA_SERRA(3, "Manacá-da-serra", "Manacá é palavra de origem tupi; 'da serra' marca onde ela cresce, nas encostas da Serra do Mar. Suas flores nascem brancas, viram rosa e terminam roxas, deixando a árvore tricolor. Vive na Mata Atlântica de SP, RJ, PR e SC."),
    MARANTA(4, "Maranta", "O gênero homenageia o botânico italiano Bartolomeo Maranta, do século XVI. É conhecida como 'planta-que-reza', porque fecha as folhas ao anoitecer como mãos em oração. Cresce na sombra do sub-bosque da Mata Atlântica e da Amazônia."),
    PACOVA(5, "Pacová", "O nome vem do tupi 'pakowa', usado para folhas largas como as da bananeira. É uma epífita de folhas grossas em forma de calha, que acumula água e folhas secas no centro. Habita a Mata Atlântica do Sudeste e do Sul."),
    PITANGUEIRA(6, "Pitangueira", "Pitanga vem do tupi 'pyrang', que quer dizer 'vermelho', a cor do seu fruto maduro. Árvore de restingas e matas da Mata Atlântica, muito presente em quintais brasileiros. Ocorre do Nordeste ao Sul do país."),
    RAINHA_DO_ABISMO(7, "Rainha-do-abismo", "Ganhou esse nome por brotar em paredões rochosos e beiras de precipícios. Guarda água e nutrientes num tubérculo lenhoso, o que a faz sobreviver na rocha quase sem terra. É endêmica dos afloramentos rochosos do Paraná."),
    TREVO_ROXO(8, "Trevo roxo", "O nome vem das três folhas em forma de coração, como as do trevo, e da cor arroxeada. Ao entardecer as folhas se dobram para baixo e se reabrem com o sol. Cresce em áreas abertas do Cerrado e do Sul e Sudeste."),
    MULUNGU(9, "Mulungu", "Mulungu é palavra de origem africana, trazida pelas línguas bantas ao português brasileiro. Suas flores vermelhas aparecem nos galhos desfolhados e espinhosos, e a casca é usada na medicina popular como calmante. Vive na Caatinga, no Cerrado e em matas do Sudeste."),
    SUCUPIRA_PRETA(10, "Sucupira-preta", "Sucupira vem do tupi e se refere à madeira escura e pesadíssima; 'preta' distingue das demais sucupiras. Árvore de casca grossa resistente ao fogo, floresce em cachos roxo-azulados. Domina o Cerrado e aparece também na Caatinga e na Amazônia."),
    ARVORE_TECNOLOGICA(11, "Árvore Tecnológica", "Planta secreta do Soulie: uma árvore digital que mistura copa, circuito e código. Só aparece para quem conhece o código NEXT.");

    private final int id;
    private final String name;
    private final String origin;

    PlantType(int id, String name, String origin) {
        this.id = id;
        this.name = name;
        this.origin = origin;
    }
}
