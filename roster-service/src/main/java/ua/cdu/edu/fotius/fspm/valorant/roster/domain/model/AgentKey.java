package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;

public record AgentKey(String value) {
    public AgentKey {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_AGENT_KEY", "AgentKey cannot be null or blank");
        }
        value = value.trim().toLowerCase();
    }
}
