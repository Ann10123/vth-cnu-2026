package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import java.util.Objects;

public class RosterMember {
    private final String id;
    private final AgentPick pick;
    private final String playerTag;
    private RoleType roleType;

    public RosterMember(String id, AgentPick pick, String playerTag, RoleType roleType) {
        this.id = Objects.requireNonNull(id);
        this.pick = Objects.requireNonNull(pick);
        this.playerTag = playerTag != null ? playerTag : pick.playerId().value();
        this.roleType = Objects.requireNonNull(roleType);
    }

    void promoteToCaptain() {
        this.roleType = RoleType.CAPTAIN;
    }

    void demoteToPlayer() {
        this.roleType = RoleType.PLAYER;
    }

    public String getId() { return id; }
    public AgentPick getPick() { return pick; }
    public int getSlot() { return pick.slot(); }
    public PlayerId getPlayerId() { return pick.playerId(); }
    public String getPlayerTag() { return playerTag; }
    public AgentKey getAgentKey() { return pick.agentKey(); }
    public RoleType getRoleType() { return roleType; }
}
