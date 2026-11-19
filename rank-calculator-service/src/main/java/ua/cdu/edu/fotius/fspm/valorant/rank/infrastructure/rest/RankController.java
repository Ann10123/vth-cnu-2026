package ua.cdu.edu.fotius.fspm.valorant.rank.infrastructure.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.validation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ua.cdu.edu.fotius.fspm.valorant.rank.domain.RatingPolicy;

import java.util.Map;

@Validated
@Controller("/internal/v1")
public class RankController {

    private final RatingPolicy ratingPolicy = RatingPolicy::defaultCompetitivePolicy;

    @Serdeable
    public record RankCalculationRequest(
        boolean isWin,

        @Min(value = 1, message = "roundMargin must be at least 1")
        @Max(value = 13, message = "roundMargin cannot exceed 13")
        int roundMargin,

        @Min(value = 0, message = "kills cannot be negative")
        @Max(value = 100, message = "kills value is unrealistically high")
        int kills,

        @Min(value = 0, message = "deaths cannot be negative")
        @Max(value = 100, message = "deaths value is unrealistically high")
        int deaths,

        @Min(value = 0, message = "assists cannot be negative")
        @Max(value = 100, message = "assists value is unrealistically high")
        int assists,

        @Min(value = 0, message = "combatScore cannot be negative")
        @Max(value = 1000, message = "combatScore cannot exceed 1000")
        int combatScore,

        boolean isAfk
    ) {}

    @Serdeable
    public record RankCalculationResponse(int deltaRR, String tierStatus) {}

    @Post("/calculate-rating")
    public HttpResponse<?> calculateRating(@Valid @Body RankCalculationRequest request) {
        if (request.deaths() > 24) {
            return HttpResponse.badRequest(Map.of(
                "code", "IMPOSSIBLE_MATCH_STATS",
                "message", "Deaths cannot exceed 24 in a regulation match"
            ));
        }

        var stats = new RatingPolicy.MatchStats(
            request.isWin(),
            request.roundMargin(),
            request.kills(),
            request.deaths(),
            request.assists(),
            request.combatScore(),
            request.isAfk()
        );
        var result = ratingPolicy.calculate(stats);
        return HttpResponse.ok(new RankCalculationResponse(result.deltaRR(), result.tierStatus()));
    }
}
