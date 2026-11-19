package ua.cdu.edu.fotius.fspm.valorant.rank.domain;

@FunctionalInterface
public interface RatingPolicy {

    record MatchStats(
        boolean isWin,
        int roundMargin,
        int kills,
        int deaths,
        int assists,
        int combatScore,
        boolean isAfk
    ) {
        public MatchStats {
            if (roundMargin < 1 || roundMargin > 13) {
                throw new IllegalArgumentException("roundMargin must be between 1 and 13");
            }
            if (kills < 0 || deaths < 0 || assists < 0 || combatScore < 0) {
                throw new IllegalArgumentException("Match statistics cannot be negative");
            }
        }
    }

    record CalculationResult(int deltaRR, String tierStatus) {}

    CalculationResult calculate(MatchStats stats);

    static CalculationResult defaultCompetitivePolicy(MatchStats stats) {
        int marginBonus = Math.min(Math.max(stats.roundMargin(), 1), 13);
        int baseRR = stats.isWin() ? (10 + marginBonus) : -(10 + marginBonus);
        double kd = stats.deaths() == 0 ? stats.kills() : (double) stats.kills() / stats.deaths();
        int performanceBonus = (stats.combatScore() >= 300 || kd > 2.0) ? 5 : 0;
        int afkPenalty = stats.isAfk() ? -10 : 0;
        int finalDelta = baseRR + performanceBonus + afkPenalty;

        String tierStatus = "MAINTAINED";
        if (finalDelta >= 25) tierStatus = "PROMOTED";
        else if (finalDelta <= -20) tierStatus = "DEMOTED";

        return new CalculationResult(finalDelta, tierStatus);
    }
}
