package ua.cdu.edu.fotius.fspm.valorant.analytics;

import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RankCalculatorClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RosterServiceClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.rest.MatchController;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@MicronautTest
class MatchControllerTest {

    @Inject
    @Client("/")
    HttpClient client;
    @Inject
    RankCalculatorClient rankCalculatorClient;
    @Inject
    RosterServiceClient rosterServiceClient;

    @MockBean(RankCalculatorClient.class)
    RankCalculatorClient mockRankCalculatorClient() {
        return Mockito.mock(RankCalculatorClient.class);
    }

    @MockBean(RosterServiceClient.class)
    RosterServiceClient mockRosterServiceClient() {
        return Mockito.mock(RosterServiceClient.class);
    }

    @Test
    @DisplayName("GIVEN match submission WHEN posted THEN calls RankCalculator and persists to DB")
    void testMatchSubmissionWithMockedInterService() {
        Mockito.when(rankCalculatorClient.calculateRating(any()))
                .thenReturn(new RankCalculatorClient.RankCalculationResponse(25, "PROMOTED"));

        var reqPayload = new MatchController.MatchSubmissionRequest("r-101", "p-001", "Ascent", true, 5, 22, 12, 5, 310,
                false);
        var req = HttpRequest.POST("/api/v1/matches/submit", reqPayload);
        var res = client.toBlocking().exchange(req, MatchController.MatchSubmissionResponse.class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertNotNull(res.body());
        assertEquals(25, res.body().deltaRR());
        assertEquals("PROMOTED", res.body().tierStatus());
    }
}
