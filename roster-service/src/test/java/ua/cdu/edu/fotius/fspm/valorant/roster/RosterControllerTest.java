package ua.cdu.edu.fotius.fspm.valorant.roster;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ua.cdu.edu.fotius.fspm.valorant.roster.infrastructure.rest.RosterController;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class RosterControllerTest {

    private static final String TEST_ROSTER_ID = "r-test-001";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    Jdbi jdbi;

    private String authToken() throws Exception {
        var loginReq = HttpRequest.POST("/api/v1/auth/login",
                "{\"playerTag\":\"ProGamer#EUW\",\"pin\":\"1234\"}")
                .header("Content-Type", "application/json");
        var loginRes = client.toBlocking().exchange(loginReq, String.class);
        assertEquals(HttpStatus.OK, loginRes.getStatus());

        String body = loginRes.body();
        int start = body.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private String createRoster(String token, String mapName) throws Exception {
        var createReq = HttpRequest.POST("/api/v1/rosters",
                "{\"mapName\":\"" + mapName + "\",\"creatorPlayerId\":\"p-001\"}")
            .header("Content-Type", "application/json")
            .header("X-Player-Id", "p-001")
            .header("Authorization", "Bearer " + token);
        var createRes = client.toBlocking().exchange(createReq, String.class);
        assertEquals(HttpStatus.CREATED, createRes.getStatus());

        String body = createRes.body();
        int start = body.indexOf("\"id\":\"") + "\"id\":\"".length();
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    @BeforeEach
    void beforeEach() {
        // Pure DB: create a fresh empty test roster for this test
        jdbi.useHandle(handle ->
            handle.createUpdate(
                "INSERT INTO rosters (id, captain_id, map_name, status) VALUES ('" + TEST_ROSTER_ID + "', 'p-001', 'Ascent', 'DRAFT')"
            ).execute()
        );
    }

    @AfterEach
    void afterEach() {
        // Pure DB: delete only the test roster; r-101 seed data is untouched
        jdbi.useHandle(handle -> {
            handle.createUpdate("DELETE FROM roster_members WHERE roster_id = '" + TEST_ROSTER_ID + "'").execute();
            handle.createUpdate("DELETE FROM rosters WHERE id = '" + TEST_ROSTER_ID + "'").execute();
        });
    }

    @Nested
    @DisplayName("GIVEN a Roster in DRAFT state")
    class DraftStateTests {

        @Test
        @DisplayName("WHEN adding a duplicate agent THEN 400 Bad Request is returned")
        void testDuplicateAgentReturns400() throws Exception {
            String token = authToken();

            // Setup via HTTP: add jett to slot 1
            var addJettReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "p-002", "jett"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addJettReq, String.class);

            // Now adding jett again triggers DUPLICATE_AGENT
            var duplicateReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(3, "p-003", "jett"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(duplicateReq, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN adding to an occupied slot THEN 409 Conflict is returned")
        void testOccupiedSlotReturns409() throws Exception {
            String token = authToken();

            // Setup via HTTP: add jett to slot 1
            var addJettReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "p-002", "jett"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addJettReq, String.class);

            // Try to add sage to occupied slot 1
            var slotReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "p-003", "sage"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(slotReq, String.class));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN adding a duplicate player THEN 400 Bad Request is returned")
        void testDuplicatePlayerReturns400() throws Exception {
            String token = authToken();

            // Setup via HTTP: add jett by p-002 to slot 1
            var addJettReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "p-002", "jett"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addJettReq, String.class);

            // Try to add p-002 again (already in roster)
            var playerReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(2, "p-002", "sage"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(playerReq, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN Captain locks in THEN status transitions to LOCKED and HAL links update")
        void testCaptainLockInBDD() throws Exception {
            String token = authToken();

            // Setup via HTTP: add jett to slot 1 and omen to slot 2
            var addJettReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "p-001", "jett"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addJettReq, String.class);

            var addOmenReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(2, "p-002", "omen"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addOmenReq, String.class);

            var getReq = HttpRequest.GET("/api/v1/rosters/" + TEST_ROSTER_ID)
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertEquals(HttpStatus.OK, getRes.getStatus());
            assertTrue(getRes.body().contains("DRAFT"));

            var lockReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/lock", "")
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

            var invalidReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                    new RosterController.AddAgentRequest(1, "", "invalid agent!"))
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(invalidReq, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
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
        @DisplayName("WHEN unarchiving an ARCHIVED roster THEN 204 and transitions to DRAFT")
        void testUnarchiveArchivedRosterReturns204() throws Exception {
            String token = authToken();
            String rosterId = createRoster(token, "Ascent");

            // Archive the roster first
            var archiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/archive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(archiveReq, String.class);

            // Unarchive should succeed
            var unarchiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/unarchive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var unarchiveRes = client.toBlocking().exchange(unarchiveReq);
            assertEquals(HttpStatus.NO_CONTENT, unarchiveRes.getStatus());

            // Verify roster is back to DRAFT
            var getReq = HttpRequest.GET("/api/v1/rosters/" + rosterId)
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertTrue(getRes.body().contains("DRAFT"));
        }

        @Test
        @DisplayName("WHEN unarchiving a non-ARCHIVED roster THEN 400 Bad Request")
        void testUnarchiveNonArchivedRosterReturns400() throws Exception {
            String token = authToken();
            String rosterId = createRoster(token, "Ascent");

            // Roster is DRAFT, not ARCHIVED — unarchive should fail
            var unarchiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/unarchive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(unarchiveReq, String.class));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN archiving a DRAFT roster THEN 204 and status transitions to ARCHIVED")
        void testArchiveRosterReturns204() throws Exception {
            String token = authToken();
            String rosterId = createRoster(token, "Ascent");

            var archiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/archive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var archiveRes = client.toBlocking().exchange(archiveReq);
            assertEquals(HttpStatus.NO_CONTENT, archiveRes.getStatus());

            // Verify roster is now ARCHIVED
            var getReq = HttpRequest.GET("/api/v1/rosters/" + rosterId)
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertTrue(getRes.body().contains("ARCHIVED"));
        }

        @Test
        @DisplayName("WHEN archiving an already-ARCHIVED roster THEN 409 Conflict is returned")
        void testArchiveAlreadyArchivedRosterReturns409() throws Exception {
            String token = authToken();
            String rosterId = createRoster(token, "Ascent");

            // Archive once
            var firstArchiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/archive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(firstArchiveReq, String.class);

            // Archive again should return 409
            var secondArchiveReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/archive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(secondArchiveReq, String.class));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        @DisplayName("WHEN transferring captain to another roster member THEN 200 OK")
        void testTransferCaptainReturns200() throws Exception {
            String token = authToken();
            String rosterId = createRoster(token, "Ascent");

            // Setup: add p-002 as a roster member so they can become captain
            var addMemberReq = HttpRequest.POST("/api/v1/rosters/" + rosterId + "/agents",
                    new RosterController.AddAgentRequest(2, "p-002", "omen"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(addMemberReq, String.class);

            // Transfer captaincy from p-001 to p-002
            var transferReq = HttpRequest.PUT("/api/v1/rosters/" + rosterId + "/captain",
                    new RosterController.TransferCaptainRequest("p-002"))
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var transferRes = client.toBlocking().exchange(transferReq);
            assertEquals(HttpStatus.OK, transferRes.getStatus());
        }

        @Test
        @DisplayName("WHEN roster has no members THEN GET returns empty members array (OpenAPI contract)")
        void testEmptyRosterReturnsMembersArray() throws Exception {
            String token = authToken();
            var getReq = HttpRequest.GET("/api/v1/rosters/" + TEST_ROSTER_ID)
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertEquals(HttpStatus.OK, getRes.getStatus());
            assertTrue(getRes.body().contains("\"members\":[]"),
                "Expected empty members array in response, got: " + getRes.body());
        }
    }

    @Nested
    @DisplayName("GIVEN HATEOAS contract: UI must render controls only from server _links")
    class HalLinksContract {

        private String getBody(String token, String rosterId) {
            var getReq = HttpRequest.GET("/api/v1/rosters/" + rosterId)
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            var getRes = client.toBlocking().exchange(getReq, String.class);
            assertEquals(HttpStatus.OK, getRes.getStatus());
            return getRes.body();
        }

        @Test
        @DisplayName("WHEN DRAFT captain with enough members THEN offers add_agent, lock_in, transfer_captain, self — and NOT lock/submit/unarchive")
        void testDraftLinks() throws Exception {
            String token = authToken();
            for (int slot = 1; slot <= 2; slot++) {
                var addReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                        new RosterController.AddAgentRequest(slot, "p-00" + (slot + 1), slot == 1 ? "jett" : "omen"))
                    .header("X-Player-Id", "p-001")
                    .header("Authorization", "Bearer " + token);
                client.toBlocking().exchange(addReq, String.class);
            }
            String body = getBody(token, TEST_ROSTER_ID);

            assertTrue(body.contains("\"self\""));
            assertTrue(body.contains("\"add_agent\""));
            assertTrue(body.contains("\"lock_in\""));
            assertTrue(body.contains("\"transfer_captain\""));
            assertFalse(body.contains("\"submit_match\""));
            assertFalse(body.contains("\"unlock\""));
            assertFalse(body.contains("\"unarchive\""));
        }

        @Test
        @DisplayName("WHEN LOCKED THEN offers unlock + submit_match (with the configured analytics href) — and NOT add_agent/lock_in")
        void testLockedLinks() throws Exception {
            String token = authToken();
            for (int slot = 1; slot <= 2; slot++) {
                var addReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/agents",
                        new RosterController.AddAgentRequest(slot, "p-00" + (slot + 1), slot == 1 ? "jett" : "omen"))
                    .header("X-Player-Id", "p-001")
                    .header("Authorization", "Bearer " + token);
                client.toBlocking().exchange(addReq, String.class);
            }
            var lockReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/lock", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(lockReq, String.class);

            String body = getBody(token, TEST_ROSTER_ID);

            assertTrue(body.contains("\"submit_match\""));
            assertTrue(body.contains("/api/v1/matches/submit"),
                "submit_match href must be server-provided, got: " + body);
            assertTrue(body.contains("\"unlock\""));
            assertFalse(body.contains("\"add_agent\""));
            assertFalse(body.contains("\"lock_in\""));
        }

        @Test
        @DisplayName("WHEN ARCHIVED THEN offers unarchive — and NOT submit_match")
        void testArchivedLinks() throws Exception {
            String token = authToken();
            var archiveReq = HttpRequest.POST("/api/v1/rosters/" + TEST_ROSTER_ID + "/archive", "")
                .header("X-Player-Id", "p-001")
                .header("Authorization", "Bearer " + token);
            client.toBlocking().exchange(archiveReq, String.class);

            String body = getBody(token, TEST_ROSTER_ID);

            assertTrue(body.contains("\"unarchive\""));
            assertTrue(body.contains("\"self\""));
            assertFalse(body.contains("\"submit_match\""));
            assertFalse(body.contains("\"lock_in\""));
        }
    }
}
