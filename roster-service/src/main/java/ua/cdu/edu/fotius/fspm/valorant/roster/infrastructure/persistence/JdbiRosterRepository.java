package ua.cdu.edu.fotius.fspm.valorant.roster.infrastructure.persistence;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.*;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.repository.RosterRepository;

import java.util.List;
import java.util.Optional;

@Singleton
public class JdbiRosterRepository implements RosterRepository {

    private final Jdbi jdbi;

    @Inject
    public JdbiRosterRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public Optional<Roster> findById(RosterId id) {
        return jdbi.withHandle(handle -> {
            var rosterOpt = handle.createQuery(
                    "SELECT id, captain_id, map_name, status FROM rosters WHERE id = :id")
                .bind("id", id.value())
                .mapToMap()
                .findOne();

            if (rosterOpt.isEmpty()) {
                return Optional.empty();
            }

            List<RosterMember> members = handle.createQuery(
                    "SELECT rm.id, rm.slot, rm.player_id, p.player_tag, rm.agent_key, rm.role_type " +
                    "FROM roster_members rm LEFT JOIN players p ON rm.player_id = p.player_id " +
                    "WHERE rm.roster_id = :id ORDER BY rm.slot ASC")
                .bind("id", id.value())
                .map((rs, ctx) -> new RosterMember(
                    rs.getString("id"),
                    new AgentPick(
                        rs.getInt("slot"),
                        new AgentKey(rs.getString("agent_key")),
                        new PlayerId(rs.getString("player_id"))
                    ),
                    rs.getString("player_tag"),
                    RoleType.valueOf(rs.getString("role_type"))
                ))
                .list();

            var data = rosterOpt.get();
            return Optional.of(new Roster(
                new RosterId((String) data.get("id")),
                new PlayerId((String) data.get("captain_id")),
                (String) data.get("map_name"),
                RosterStatus.valueOf((String) data.get("status")),
                members
            ));
        });
    }

    @Override
    public Optional<String> findPlayerTagById(PlayerId playerId) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT player_tag FROM players WHERE player_id = :pid")
                .bind("pid", playerId.value())
                .mapTo(String.class)
                .findOne()
        );
    }

    @Override
    public Optional<PlayerId> findPlayerIdByTag(String playerTag) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT player_id FROM players WHERE player_tag = :tag")
                .bind("tag", playerTag)
                .mapTo(String.class)
                .findOne()
                .map(PlayerId::new)
        );
    }

    @Override
    public void save(Roster roster) {
        jdbi.useTransaction(handle -> {
            int updated = handle.createUpdate(
                    "UPDATE rosters SET captain_id = :cid, map_name = :map, status = :status WHERE id = :id")
                .bind("id", roster.getId().value())
                .bind("cid", roster.getCaptainId().value())
                .bind("map", roster.getMapName())
                .bind("status", roster.getStatus().name())
                .execute();

            if (updated == 0) {
                handle.createUpdate(
                        "INSERT INTO rosters (id, captain_id, map_name, status) VALUES (:id, :cid, :map, :status)")
                    .bind("id", roster.getId().value())
                    .bind("cid", roster.getCaptainId().value())
                    .bind("map", roster.getMapName())
                    .bind("status", roster.getStatus().name())
                    .execute();
            }

            handle.createUpdate("DELETE FROM roster_members WHERE roster_id = :rid")
                .bind("rid", roster.getId().value())
                .execute();

            for (RosterMember member : roster.getMembers()) {
                handle.createUpdate(
                        "INSERT INTO roster_members (id, roster_id, slot, player_id, agent_key, role_type) " +
                        "VALUES (:mid, :rid, :slot, :pid, :agent, :role)")
                    .bind("mid", member.getId())
                    .bind("rid", roster.getId().value())
                    .bind("slot", member.getSlot())
                    .bind("pid", member.getPlayerId().value())
                    .bind("agent", member.getAgentKey().value())
                    .bind("role", member.getRoleType().name())
                    .execute();
            }
        });
    }
}
