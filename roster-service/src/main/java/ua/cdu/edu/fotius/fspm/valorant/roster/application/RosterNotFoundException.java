package ua.cdu.edu.fotius.fspm.valorant.roster.application;

import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.RosterId;

public class RosterNotFoundException extends RuntimeException {
    public RosterNotFoundException(RosterId id) {
        super("Roster not found: " + id.value());
    }
}
