package ua.cdu.edu.fotius.fspm.valorant.roster.domain.repository;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.PlayerId;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.Roster;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.RosterId;

import java.util.Optional;

public interface RosterRepository {
    Optional<Roster> findById(RosterId id);
    Optional<String> findPlayerTagById(PlayerId playerId);
    Optional<PlayerId> findPlayerIdByTag(String playerTag);
    void save(Roster roster);
}
