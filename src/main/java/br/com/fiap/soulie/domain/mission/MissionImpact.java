package br.com.fiap.soulie.domain.mission;

/**
 * Médias por ação concluída. Não são medições da foto: são referências públicas
 * arredondadas para uma ação típica de um dia.
 *
 * Reciclagem: cerca de 300 g, a fração reciclável de ~1 kg de resíduo urbano por pessoa/dia (ABRELPE).
 * Cada quilo de mistura (papel, plástico e metal) evita por volta de 1,1 kg de CO₂e.
 * Transporte: ida e volta de 8 km. Carro a gasolina ~170 g/km; ônibus ~70 g por passageiro/km.
 * A diferença fica em cerca de 1,6 kg de CO₂.
 * Banho: 10 minutos a menos num chuveiro de 6 L/min = 60 L. Aquecer isso gasta ~1,4 kWh;
 * a rede elétrica brasileira emite cerca de 0,09 kg de CO₂/kWh.
 * Embalagem evitada: ~80 g de plástico, e produzir 1 kg de plástico emite cerca de 3 kg de CO₂.
 * Standby: um conjunto de aparelhos por um dia, ~1,2 kWh.
 * Compostagem: ~500 g de restos. No aterro, 1 kg de comida vira cerca de 1 kg de CO₂e em metano.
 * Refeição sem carne: troca de uma refeição média com carne, não de um prato só de carne bovina (~1,5 kg).
 * Luz natural: algumas lâmpadas de LED apagadas por algumas horas.
 * Muda: cota conservadora de 2 kg de CO₂ no primeiro ano de uma árvore jovem, não absorção imediata.
 * Reutilizável: dois copos ou garrafas descartáveis deixados de usar no dia.
 */
public final class MissionImpact {

    public record Factors(double recycledKg, double co2Kg, double waterLiters) {
    }

    private MissionImpact() {
    }

    public static Factors of(String missionName) {
        if (missionName == null) {
            return new Factors(0, 0, 0);
        }
        return switch (missionName) {
            case "Reciclar Resíduos" -> new Factors(0.30, 0.35, 0);
            case "Transporte Público" -> new Factors(0, 1.60, 0);
            case "Economia de Água" -> new Factors(0, 0.12, 60);
            case "Compra Consciente" -> new Factors(0, 0.25, 0);
            case "Energia Limpa" -> new Factors(0, 0.12, 0);
            case "Compostagem" -> new Factors(0, 0.50, 0);
            case "Refeição Sem Carne" -> new Factors(0, 1.50, 0);
            case "Luz Natural" -> new Factors(0, 0.04, 0);
            case "Plantar uma Muda" -> new Factors(0, 2.00, 0);
            case "Zero Descartáveis" -> new Factors(0, 0.15, 0);
            default -> new Factors(0, 0, 0);
        };
    }
}
