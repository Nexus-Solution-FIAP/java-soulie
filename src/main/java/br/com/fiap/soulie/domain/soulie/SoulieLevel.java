package br.com.fiap.soulie.domain.soulie;

public final class SoulieLevel {

    private SoulieLevel() {
    }

    public static int levelOf(int xp) {
        if (xp < 50) {
            return 0;
        }
        if (xp < 100) {
            return 1;
        }
        if (xp < 200) {
            return 2;
        }
        return 3;
    }

    public static String nameOf(int level) {
        return switch (level) {
            case 0 -> "Semente";
            case 1 -> "Broto";
            case 2 -> "Planta Jovem";
            default -> "Árvore";
        };
    }

    public static int nextXp(int level) {
        return switch (level) {
            case 0 -> 50;
            case 1 -> 100;
            case 2 -> 200;
            default -> 500;
        };
    }
}
