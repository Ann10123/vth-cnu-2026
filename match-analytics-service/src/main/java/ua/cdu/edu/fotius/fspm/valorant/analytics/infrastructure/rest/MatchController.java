package ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RankCalculatorClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RosterServiceClient;

import java.util.*;

@Controller("/api/v1/matches")
public class MatchController {

    private final Jdbi jdbi;
    private final RankCalculatorClient rankClient;
    private final RosterServiceClient rosterClient;

    @Inject
    public MatchController(Jdbi jdbi, RankCalculatorClient rankClient, RosterServiceClient rosterClient) {
        this.jdbi = jdbi;
        this.rankClient = rankClient;
        this.rosterClient = rosterClient;
    }

    @Serdeable public record MatchSubmissionRequest(String rosterId, String playerId, String mapName, boolean isWin, int roundMargin, int kills, int deaths, int assists, int combatScore, boolean isAfk) {}
    @Serdeable public record MatchSubmissionResponse(String matchId, int deltaRR, String tierStatus) {}
    @Serdeable public record MatchHistoryItemDto(String matchId, String mapName, String result, int deltaRR, String tierStatus) {}

    @Post("/submit")
    public MatchSubmissionResponse submitMatch(@Body MatchSubmissionRequest request) {
        String matchId = UUID.randomUUID().toString();

        var rankRes = rankClient.calculateRating(new RankCalculatorClient.RankCalculationRequest(
            request.isWin(), request.roundMargin(), request.kills(),
            request.deaths(), request.assists(), request.combatScore(), request.isAfk()
        ));

        jdbi.useTransaction(handle -> {
            handle.createUpdate("INSERT INTO matches (id, roster_id, map_name, result, round_margin) VALUES (:id, :rid, :map, :res, :margin)")
                .bind("id", matchId)
                .bind("rid", request.rosterId())
                .bind("map", request.mapName())
                .bind("res", request.isWin() ? "WIN" : "LOSS")
                .bind("margin", request.roundMargin())
                .execute();

            handle.createUpdate("INSERT INTO match_players (id, match_id, player_id, kills, deaths, assists, combat_score, delta_rr, tier_status) " +
                                "VALUES (:id, :mid, :pid, :k, :d, :a, :score, :rr, :tier)")
                .bind("id", UUID.randomUUID().toString())
                .bind("mid", matchId)
                .bind("pid", request.playerId())
                .bind("k", request.kills())
                .bind("d", request.deaths())
                .bind("a", request.assists())
                .bind("score", request.combatScore())
                .bind("rr", rankRes.deltaRR())
                .bind("tier", rankRes.tierStatus())
                .execute();
        });

        try {
            rosterClient.archiveRoster(request.rosterId());
        } catch (Exception e) {}

        return new MatchSubmissionResponse(matchId, rankRes.deltaRR(), rankRes.tierStatus());
    }

    @Get("/history/{playerId}")
    public HttpResponse<List<MatchHistoryItemDto>> getMatchHistory(String playerId) {
        List<MatchHistoryItemDto> history = jdbi.withHandle(handle ->
            handle.createQuery("SELECT m.id, m.map_name, m.result, mp.delta_rr, mp.tier_status " +
                               "FROM matches m JOIN match_players mp ON m.id = mp.match_id " +
                               "WHERE mp.player_id = :pid ORDER BY m.played_at DESC")
                .bind("pid", playerId)
                .map((rs, ctx) -> new MatchHistoryItemDto(
                    rs.getString("id"),
                    rs.getString("map_name"),
                    rs.getString("result"),
                    rs.getInt("delta_rr"),
                    rs.getString("tier_status")
                ))
                .list()
        );
        return HttpResponse.ok(history);
    }
}
