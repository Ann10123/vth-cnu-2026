package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;

public record PlayerId(String value) {
    public PlayerId {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_PLAYER_ID", "PlayerId cannot be null or blank");
        }
        value = value.trim();
    }
}
