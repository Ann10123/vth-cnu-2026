package ua.cdu.edu.fotius.fspm.valorant.roster.application;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.*;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.repository.RosterRepository;

@Singleton
public class RosterApplicationService {

    private final RosterRepository rosterRepository;

    @Inject
    public RosterApplicationService(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    public Roster createRoster(String mapName, PlayerId creatorId) {
        Roster roster = Roster.createDraft(RosterId.generate(), creatorId, mapName);
        rosterRepository.save(roster);
        return roster;
    }

    public Roster getRoster(RosterId rosterId) {
        return rosterRepository.findById(rosterId)
            .orElseThrow(() -> new RosterNotFoundException(rosterId));
    }

    public void addAgent(RosterId rosterId, Integer requestedSlot, PlayerId playerId, AgentKey agentKey) {
        Roster roster = getRoster(rosterId);
        int slot = (requestedSlot != null) ? requestedSlot : roster.nextAvailableSlot();
        AgentPick pick = new AgentPick(slot, agentKey, playerId);
        String playerTag = rosterRepository.findPlayerTagById(playerId).orElse(playerId.value());
        roster.addAgent(pick, playerTag);
        rosterRepository.save(roster);
    }

    public void removeAgent(RosterId rosterId, AgentKey agentKey) {
        Roster roster = getRoster(rosterId);
        roster.removeMemberByAgent(agentKey);
        rosterRepository.save(roster);
    }

    public void transferCaptain(RosterId rosterId, PlayerId actorId, PlayerId newCaptainId) {
        Roster roster = getRoster(rosterId);
        roster.transferCaptain(actorId, newCaptainId);
        rosterRepository.save(roster);
    }

    public void lockIn(RosterId rosterId, PlayerId actorId) {
        Roster roster = getRoster(rosterId);
        roster.lockIn(actorId);
        rosterRepository.save(roster);
    }

    public void unlock(RosterId rosterId, PlayerId actorId) {
        Roster roster = getRoster(rosterId);
        roster.unlock(actorId);
        rosterRepository.save(roster);
    }

    public void archive(RosterId rosterId) {
        Roster roster = getRoster(rosterId);
        roster.archive();
        rosterRepository.save(roster);
    }

    public void unarchive(RosterId rosterId) {
        Roster roster = getRoster(rosterId);
        roster.unarchive();
        rosterRepository.save(roster);
    }

    public PlayerId findPlayerIdByTag(String playerTag) {
        return rosterRepository.findPlayerIdByTag(playerTag)
            .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerTag));
    }
}
