package br.com.fiap.soulie.domain.plant;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;

public enum PlantCatalog {
    IPE_AMARELO(FlowerType.IPE_AMARELO, "Ipê Amarelo", true, PlantUnlockKind.MISSIONS, 5, null, "Complete 5 missões"),
    GUAIMBE_ONDULADO(FlowerType.GUAIMBE_ONDULADO, "Guaimbê-ondulado", true, PlantUnlockKind.MISSIONS, 8, null, "Complete 8 missões"),
    MANACA_DA_SERRA(FlowerType.MANACA_DA_SERRA, "Manacá-da-serra", true, PlantUnlockKind.ACHIEVEMENTS, 3, null, "Desbloqueie 3 conquistas"),
    MARANTA(FlowerType.MARANTA, "Maranta", true, PlantUnlockKind.MISSIONS, 12, null, "Complete 12 missões"),
    PACOVA(FlowerType.PACOVA, "Pacová", true, PlantUnlockKind.ACHIEVEMENTS, 5, null, "Desbloqueie 5 conquistas"),
    PITANGUEIRA(FlowerType.PITANGUEIRA, "Pitangueira", true, PlantUnlockKind.MAX_PLANTS, 1, null, "Leve 1 planta ao nível máximo"),
    RAINHA_DO_ABISMO(FlowerType.RAINHA_DO_ABISMO, "Rainha-do-abismo", true, PlantUnlockKind.STREAK_DAYS, 7, null, "Mantenha 7 dias de sequência"),
    TREVO_ROXO(FlowerType.TREVO_ROXO, "Trevo roxo", true, PlantUnlockKind.MISSIONS, 20, null, "Complete 20 missões"),
    MULUNGU(FlowerType.MULUNGU, "Mulungu", true, PlantUnlockKind.ACHIEVEMENTS, 7, null, "Desbloqueie 7 conquistas"),
    SUCUPIRA_PRETA(FlowerType.SUCUPIRA_PRETA, "Sucupira-preta", true, PlantUnlockKind.MAX_PLANTS, 2, null, "Leve 2 plantas ao nível máximo"),
    GIRASSOL(FlowerType.GIRASSOL, "Girassol", false, PlantUnlockKind.MISSIONS, 15, null, "Complete 15 missões"),
    ROSA(FlowerType.ROSA, "Rosa", false, PlantUnlockKind.SECRET_CODE, 0, "ROSAFLORA", "Use um código secreto"),
    HIBISCO(FlowerType.HIBISCO, "Hibisco", false, PlantUnlockKind.ACHIEVEMENTS, 10, null, "Desbloqueie 10 conquistas"),
    LAVANDA(FlowerType.LAVANDA, "Lavanda", false, PlantUnlockKind.MAX_PLANTS, 1, null, "Leve 1 planta ao nível máximo"),
    MARGARIDA(FlowerType.MARGARIDA, "Margarida", false, PlantUnlockKind.MISSIONS, 30, null, "Complete 30 missões"),
    ORQUIDEA(FlowerType.ORQUIDEA, "Orquídea", false, PlantUnlockKind.SECRET_CODE, 0, "ORQUIDEA26", "Use um código secreto");

    private final FlowerType flower;
    private final String label;
    private final boolean starterEligible;
    private final PlantUnlockKind kind;
    private final int target;
    private final String secretCode;
    private final String hint;

    PlantCatalog(FlowerType flower, String label, boolean starterEligible,
                 PlantUnlockKind kind, int target, String secretCode, String hint) {
        this.flower = flower;
        this.label = label;
        this.starterEligible = starterEligible;
        this.kind = kind;
        this.target = target;
        this.secretCode = secretCode;
        this.hint = hint;
    }

    public FlowerType flower() {
        return flower;
    }

    public String label() {
        return label;
    }

    public boolean starterEligible() {
        return starterEligible;
    }

    public PlantUnlockKind kind() {
        return kind;
    }

    public int target() {
        return target;
    }

    public String secretCode() {
        return secretCode;
    }

    public String hint() {
        return hint;
    }

    public static PlantCatalog of(FlowerType flower) {
        for (PlantCatalog item : values()) {
            if (item.flower == flower) {
                return item;
            }
        }
        throw new IllegalArgumentException("Planta fora do catálogo: " + flower);
    }

    public static PlantCatalog bySecret(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String code = raw.trim().replace(" ", "").toUpperCase();
        for (PlantCatalog item : values()) {
            if (item.secretCode != null && item.secretCode.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}
