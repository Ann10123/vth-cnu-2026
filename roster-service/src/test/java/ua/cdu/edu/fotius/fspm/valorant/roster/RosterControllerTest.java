package ua.cdu.edu.fotius.fspm.valorant.roster;

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
import ua.cdu.edu.fotius.fspm.valorant.roster.infrastructure.rest.RosterController;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class RosterControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    private String authToken() throws Exception {
        var loginReq = HttpRequest.POST("/api/v1/auth/login",
                "{\"playerTag\":\"ProGamer#EUW\",\"pin\":\"1234\"}")
                .header("Content-Type", "application/json");
        var loginRes = client.toBlocking().exchange(loginReq, String.class);
        assertEquals(HttpStatus.OK, loginRes.getStatus());

        // Extract accessToken from JSON: {"accessToken":"jwt-token-for-ProGamer#EUW","tokenType":"Bearer","expiresIn":3600}
        String body = loginRes.body();
        int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @Nested
    @DisplayName("GIVEN a Roster in DRAFT state")
    class DraftStateTests {

        @Test
        @DisplayName("WHEN roster has no members THEN GET returns empty members array (OpenAPI contract)")
        void testEmptyRosterReturnsMembersArray() throws Exception {
            String token = authToken();

            var createReq = HttpRequest.POST("/api/v1/rosters",
                    "{\"mapName\":\"Bind\",\"creatorPlayerId\":\"p-001\"}")
                .header("Content-Type", "application/json")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var createRes = client.toBlocking().exchange(createReq, String.class);
            assertEquals(HttpStatus.CREATED, createRes.getStatus());

            // A brand-new roster has zero members: the JSON MUST contain "members":[] and NOT omit the field.
            String body = createRes.body();
            assertTrue(body.contains("\"members\":[]"),
                "Expected empty members array in response, got: " + body);
        }

        @Test
        @DisplayName("WHEN Captain locks in THEN status transitions to LOCKED and HAL links update")
        void testCaptainLockInBDD() throws Exception {
            String token = authToken();

            var getReq = HttpRequest.GET("/api/v1/rosters/r-101")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertEquals(HttpStatus.OK, getRes.getStatus());
            assertTrue(getRes.body().contains("DRAFT"));

            var lockReq = HttpRequest.POST("/api/v1/rosters/r-101/lock", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var lockRes = client.toBlocking().exchange(lockReq);
            assertEquals(HttpStatus.NO_CONTENT, lockRes.getStatus());

            var updatedRes = client.toBlocking().exchange(getReq, String.class);
            assertTrue(updatedRes.body().contains("LOCKED"));
            assertTrue(updatedRes.body().contains("submit_match"));
        }

        @Test
        @DisplayName("WHEN invalid payload is posted to /agents THEN validation rejects with 400 Bad Request")
        void testEndpointValidationRejectsBlankFields() throws Exception {
            String token = authToken();

            var invalidReq = HttpRequest.POST("/api/v1/rosters/r-101/agents",
                    new RosterController.AddAgentRequest(1, "", "invalid agent!"))
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(invalidReq, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN request with invalid token THEN 401 Unauthorized")
        void testInvalidTokenReturns401() throws Exception {
            var req = HttpRequest.GET("/api/v1/rosters/r-101")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer bad-token-xyz");
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(req, String.class));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN request without Authorization header THEN 401 Unauthorized")
        void testMissingTokenReturns401() throws Exception {
            var req = HttpRequest.GET("/api/v1/rosters/r-101")
                .header("X-Player-Id", "p-001");
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(req, String.class));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }
    }
}
