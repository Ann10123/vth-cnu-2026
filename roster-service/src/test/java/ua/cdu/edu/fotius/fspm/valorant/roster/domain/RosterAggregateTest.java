package ua.cdu.edu.fotius.fspm.valorant.roster.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pure Domain Unit Tests: AgentPick Value Object & Roster Aggregate")
class RosterAggregateTest {

    @Test
    @DisplayName("GIVEN AgentPick Value Object WHEN slot is outside 1..5 THEN throws INVALID_SLOT")
    void shouldValidateAgentPickSlotRange() {
        DomainException ex = assertThrows(DomainException.class,
            () -> new AgentPick(6, new AgentKey("jett"), new PlayerId("p-001")));
        assertEquals("INVALID_SLOT", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN two identical AgentPick instances WHEN compared THEN are equal by value")
    void shouldCompareAgentPicksByValue() {
        var pick1 = new AgentPick(1, new AgentKey("Jett"), new PlayerId("p-001"));
        var pick2 = new AgentPick(1, new AgentKey("  jett "), new PlayerId("p-001"));
        assertEquals(pick1, pick2);
        assertEquals(pick1.hashCode(), pick2.hashCode());
    }

    @Test
    @DisplayName("GIVEN a DRAFT roster WHEN non-captain tries to lockIn THEN throws FORBIDDEN_NOT_CAPTAIN")
    void shouldForbidNonCaptainFromLocking() {
        Roster roster = Roster.createDraft(new RosterId("r-1"), new PlayerId("p-001"), "Ascent");
        roster.addAgent(new AgentPick(1, new AgentKey("jett"), new PlayerId("p-001")), "Cap#1");
        roster.addAgent(new AgentPick(2, new AgentKey("omen"), new PlayerId("p-002")), "Plr#2");

        DomainException ex = assertThrows(DomainException.class, () -> roster.lockIn(new PlayerId("p-002")));
        assertEquals("FORBIDDEN_NOT_CAPTAIN", ex.getCode());
    }

    @Test
    @DisplayName("GIVEN a DRAFT roster WHEN duplicate agent is added THEN throws DUPLICATE_AGENT")
    void shouldRejectDuplicateAgents() {
        Roster roster = Roster.createDraft(new RosterId("r-1"), new PlayerId("p-001"), "Bind");
        roster.addAgent(new AgentPick(1, new AgentKey("Jett"), new PlayerId("p-001")), "Cap#1");

        DomainException ex = assertThrows(DomainException.class,
            () -> roster.addAgent(new AgentPick(2, new AgentKey("  jett "), new PlayerId("p-002")), "Plr#2"));
        assertEquals("DUPLICATE_AGENT", ex.getCode());
    }
}
