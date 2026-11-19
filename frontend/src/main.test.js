import { describe, it, expect, beforeEach } from 'vitest';
import { renderRosterUI } from './main.js';

describe('HATEOAS UI Contract Tests (ua.cdu.edu.fotius.fspm.valorant)', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="auth-box"></div>
            <div id="user-select"></div>
            <div id="roster-header"></div>
            <div id="agents-grid"></div>
            <div id="hateoas-actions"></div>
        `;
    });

    it('GIVEN Roster response WITH _links.lock_in THEN renders Lock In button in DOM', () => {
        const mockRoster = {
            id: "r-101", mapName: "Ascent", status: "DRAFT", members: [],
            _links: { lock_in: { href: "/api/v1/rosters/r-101/lock", method: "POST" } }
        };
        renderRosterUI(mockRoster);
        const lockBtn = document.getElementById('lock-btn');
        expect(lockBtn).not.toBeNull();
        expect(lockBtn.textContent).toBe('LOCK IN ROSTER');
    });

    it("GIVEN null roster with reason 'unauthenticated' THEN shows Not Logged In prompt and pulses auth-box", () => {
        renderRosterUI(null, 'unauthenticated');
        const header = document.getElementById('roster-header');
        expect(header.textContent).toMatch(/Not Logged In/i);
        const gotoBtn = document.getElementById('goto-login-btn');
        expect(gotoBtn).not.toBeNull();
        expect(document.getElementById('auth-box').className).toMatch(/animate-pulse/);
    });

    it("GIVEN null roster with reason 'error' THEN clears any pulse and shows no login prompt", () => {
        renderRosterUI(null, 'error');
        const header = document.getElementById('roster-header');
        expect(header.innerHTML).toBe('');
        expect(document.getElementById('auth-box').className).not.toMatch(/animate-pulse/);
    });

    it('GIVEN authenticated roster THEN clears auth-box pulse left over from a prior prompt', () => {
        renderRosterUI(null, 'unauthenticated');
        expect(document.getElementById('auth-box').className).toMatch(/animate-pulse/);
        const mockRoster = {
            id: "r-101", mapName: "Ascent", status: "DRAFT", members: [],
            _links: {}
        };
        renderRosterUI(mockRoster);
        expect(document.getElementById('auth-box').className).not.toMatch(/animate-pulse/);
    });

    it('GIVEN empty DRAFT roster (members: []) THEN renders 5 EMPTY slot placeholders', () => {
        const mockRoster = {
            id: "r-101", mapName: "Ascent", status: "DRAFT", members: [],
            _links: { self: { href: "/api/v1/rosters/r-101", method: "GET" } }
        };
        renderRosterUI(mockRoster);
        const grid = document.getElementById('agents-grid');
        expect(grid.innerHTML).toContain('SLOT #1 EMPTY');
        expect(grid.innerHTML).toContain('SLOT #5 EMPTY');
    });

    it('GIVEN roster response with MISSING members field (malformed) THEN does not crash and renders empty slots', () => {
        const malformedRoster = {
            id: "r-101", mapName: "Ascent", status: "DRAFT",
            _links: { self: { href: "/api/v1/rosters/r-101", method: "GET" } }
        };
        expect(() => renderRosterUI(malformedRoster)).not.toThrow();
        expect(document.getElementById('agents-grid').innerHTML).toContain('SLOT #1 EMPTY');
    });
});
