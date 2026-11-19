package ua.cdu.edu.fotius.fspm.valorant.rank.domain;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ua.cdu.edu.fotius.fspm.valorant.rank.infrastructure.rest.RankController;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("Competitive Rating Policy & Endpoint Validation (ua.cdu.edu.fotius.fspm.valorant)")
class RankCalculatorBDDTest {

    private final RatingPolicy policy = RatingPolicy::defaultCompetitivePolicy;

    @Inject
    @Client("/")
    HttpClient client;

    @Nested
    @DisplayName("GIVEN a Victory match")
    class VictoryTests {
        @Test
        @DisplayName("WHEN player has MVP combat score (>=300) THEN applies +5 performance bonus")
        void testWinWithMvpBonus() {
            var stats = new RatingPolicy.MatchStats(true, 8, 24, 10, 5, 320, false);
            var result = policy.calculate(stats);
            assertTrue(result.deltaRR() >= 23);
        }

        @Test
        @DisplayName("WHEN roundMargin is out of bounds (>13) THEN endpoint returns 400 Bad Request")
        void testEndpointRejectsInvalidRoundMargin() {
            var invalidPayload = new RankController.RankCalculationRequest(true, 20, 15, 10, 4, 250, false);
            var req = HttpRequest.POST("/internal/v1/calculate-rating", invalidPayload);

            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(req, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }
    }
}
