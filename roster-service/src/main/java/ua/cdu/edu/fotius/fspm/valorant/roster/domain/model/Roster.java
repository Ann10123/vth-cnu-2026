package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;
import java.util.*;

public class Roster {
    public static final int MAX_MEMBERS = 5;
    public static final int MIN_MEMBERS_TO_LOCK = 2;

    private final RosterId id;
    private PlayerId captainId;
    private final String mapName;
    private RosterStatus status;
    private final List<RosterMember> members;

    public Roster(RosterId id, PlayerId captainId, String mapName, RosterStatus status, List<RosterMember> members) {
        if (mapName == null || mapName.isBlank()) {
            throw new DomainException("INVALID_MAP_NAME", "Map name is required");
        }
        this.id = Objects.requireNonNull(id);
        this.captainId = Objects.requireNonNull(captainId);
        this.mapName = mapName.trim();
        this.status = Objects.requireNonNull(status);
        this.members = new ArrayList<>(members != null ? members : List.of());
    }

    public static Roster createDraft(RosterId id, PlayerId creatorId, String mapName) {
        return new Roster(id, creatorId, mapName, RosterStatus.DRAFT, new ArrayList<>());
    }

    public void addAgent(AgentPick pick, String playerTag) {
        ensureDraftState("add agent");

        if (members.size() >= MAX_MEMBERS) {
            throw new DomainException("ROSTER_FULL", "Roster cannot exceed " + MAX_MEMBERS + " members");
        }
        if (members.stream().anyMatch(m -> m.getSlot() == pick.slot())) {
            throw new DomainException("SLOT_OCCUPIED", "Slot " + pick.slot() + " is already occupied");
        }
        if (members.stream().anyMatch(m -> m.getPlayerId().equals(pick.playerId()))) {
            throw new DomainException("DUPLICATE_PLAYER", "Player " + pick.playerId().value() + " is already in the roster");
        }
        if (members.stream().anyMatch(m -> m.getAgentKey().equals(pick.agentKey()))) {
            throw new DomainException("DUPLICATE_AGENT", "Agent " + pick.agentKey().value() + " is already picked in this roster");
        }

        RoleType role = pick.playerId().equals(captainId) ? RoleType.CAPTAIN : RoleType.PLAYER;
        members.add(new RosterMember(UUID.randomUUID().toString(), pick, playerTag, role));
    }

    public int nextAvailableSlot() {
        for (int slot = 1; slot <= MAX_MEMBERS; slot++) {
            final int candidate = slot;
            if (members.stream().noneMatch(m -> m.getSlot() == candidate)) {
                return candidate;
            }
        }
        throw new DomainException("ROSTER_FULL", "No available slots left in the roster");
    }

    public void removeMemberByAgent(AgentKey agentKey) {
        ensureDraftState("remove agent");
        boolean removed = members.removeIf(m -> m.getAgentKey().equals(agentKey));
        if (!removed) {
            throw new DomainException("NOT_FOUND_AGENT", "Agent " + agentKey.value() + " is not in the roster");
        }
    }

    public void transferCaptain(PlayerId actorId, PlayerId newCaptainId) {
        ensureCaptain(actorId);
        ensureDraftState("transfer captain");

        RosterMember newCaptainMember = members.stream()
            .filter(m -> m.getPlayerId().equals(newCaptainId))
            .findFirst()
            .orElseThrow(() -> new DomainException("NOT_FOUND_MEMBER", "New captain must be a current member of the roster"));

        members.stream()
            .filter(m -> m.getPlayerId().equals(this.captainId))
            .findFirst()
            .ifPresent(RosterMember::demoteToPlayer);

        newCaptainMember.promoteToCaptain();
        this.captainId = newCaptainId;
    }

    public void lockIn(PlayerId actorId) {
        ensureCaptain(actorId);
        ensureDraftState("lock roster");

        if (members.size() < MIN_MEMBERS_TO_LOCK) {
            throw new DomainException("INSUFFICIENT_MEMBERS", "At least " + MIN_MEMBERS_TO_LOCK + " members are required to lock in");
        }
        this.status = RosterStatus.LOCKED;
    }

    public void unlock(PlayerId actorId) {
        ensureCaptain(actorId);
        if (this.status != RosterStatus.LOCKED) {
            throw new DomainException("INVALID_STATE_TRANSITION", "Only LOCKED roster can be unlocked back to DRAFT");
        }
        this.status = RosterStatus.DRAFT;
    }

    public void archive() {
        if (this.status == RosterStatus.ARCHIVED) {
            throw new DomainException("ROSTER_ALREADY_ARCHIVED", "Roster " + id.value() + " is already archived");
        }
        this.status = RosterStatus.ARCHIVED;
    }

    public void unarchive() {
        if (this.status != RosterStatus.ARCHIVED) {
            throw new DomainException("INVALID_STATE_TRANSITION", "Only ARCHIVED roster can be unarchived back to DRAFT");
        }
        this.status = RosterStatus.DRAFT;
    }

    private void ensureCaptain(PlayerId actorId) {
        if (!this.captainId.equals(actorId)) {
            throw new DomainException("FORBIDDEN_NOT_CAPTAIN", "Only roster captain (" + captainId.value() + ") can perform this action");
        }
    }

    private void ensureDraftState(String action) {
        if (this.status != RosterStatus.DRAFT) {
            throw new DomainException("ROSTER_NOT_IN_DRAFT", "Cannot " + action + " when roster status is " + this.status);
        }
    }

    public boolean isCaptain(PlayerId playerId) {
        return this.captainId.equals(playerId);
    }

    public RosterId getId() { return id; }
    public PlayerId getCaptainId() { return captainId; }
    public String getMapName() { return mapName; }
    public RosterStatus getStatus() { return status; }
    public List<RosterMember> getMembers() {
        return members.stream()
            .sorted(Comparator.comparingInt(RosterMember::getSlot))
            .toList();
    }
}
