package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;
import java.util.Objects;

public record AgentPick(int slot, AgentKey agentKey, PlayerId playerId) {

    public AgentPick {
        if (slot < 1 || slot > 5) {
            throw new DomainException("INVALID_SLOT", "Slot must be between 1 and 5");
        }
        Objects.requireNonNull(agentKey, "AgentKey cannot be null");
        Objects.requireNonNull(playerId, "PlayerId cannot be null");
    }
}
