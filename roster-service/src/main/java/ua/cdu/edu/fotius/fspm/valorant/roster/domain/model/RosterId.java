package ua.cdu.edu.fotius.fspm.valorant.roster.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;
import java.util.UUID;

public record RosterId(String value) {
    public RosterId {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_ROSTER_ID", "RosterId cannot be null or blank");
        }
        value = value.trim();
    }

    public static RosterId generate() {
        return new RosterId("r-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
