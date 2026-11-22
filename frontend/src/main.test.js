import { describe, it, expect, beforeEach } from 'vitest';
import { renderRosterUI, isValidMatchResult, resolveHalUrl } from './main.js';

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

    it('GIVEN ARCHIVED roster THEN renders UNARCHIVE ROSTER button', () => {
        const mockRoster = {
            id: "r-101", mapName: "Ascent", status: "ARCHIVED", members: [],
            _links: { unarchive: { href: "/api/v1/rosters/r-101/unarchive", method: "POST" } }
        };
        renderRosterUI(mockRoster);
        const unarchiveBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent === 'UNARCHIVE ROSTER');
        expect(unarchiveBtn).not.toBeNull();
        expect(unarchiveBtn.className).toContain('bg-blue-600');
    });

    it('GIVEN DRAFT roster THEN does NOT render UNARCHIVE ROSTER button', () => {
        const mockRoster = {
            id: "r-101", mapName: "Ascent", status: "DRAFT", members: [],
            _links: {}
        };
        renderRosterUI(mockRoster);
        const unarchiveBtn = Array.from(document.querySelectorAll('button')).find(b => b.textContent === 'UNARCHIVE ROSTER');
        expect(unarchiveBtn).toBeUndefined();
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

describe('Match submission result validation (no false success card)', () => {
    it('GIVEN a well-formed result THEN it is valid', () => {
        expect(isValidMatchResult({ matchId: 'm-1', deltaRR: 17, tierStatus: 'MAINTAINED' })).toBe(true);
    });

    it('GIVEN negative deltaRR THEN still valid', () => {
        expect(isValidMatchResult({ deltaRR: -12, tierStatus: 'DEMOTE' })).toBe(true);
    });

    it('GIVEN error body (no deltaRR/tierStatus) THEN invalid -> must NOT render green', () => {
        const errorBody = { _embedded: { errors: [{ message: 'Roster must be LOCKED' }] } };
        expect(isValidMatchResult(errorBody)).toBe(false);
    });

    it('GIVEN deltaRR missing or non-numeric THEN invalid', () => {
        expect(isValidMatchResult({ tierStatus: 'MAINTAINED' })).toBe(false);
        expect(isValidMatchResult({ deltaRR: 'not-a-number', tierStatus: 'MAINTAINED' })).toBe(false);
        expect(isValidMatchResult({ deltaRR: NaN, tierStatus: 'MAINTAINED' })).toBe(false);
    });

    it('GIVEN null/undefined body THEN invalid', () => {
        expect(isValidMatchResult(null)).toBe(false);
        expect(isValidMatchResult(undefined)).toBe(false);
        expect(isValidMatchResult({})).toBe(false);
    });
});

describe('resolveHalUrl (follow links, never rebuild routes)', () => {
    it('GIVEN an absolute cross-service href THEN used verbatim (frontend need not know the host)', () => {
        expect(resolveHalUrl('http://analytics.internal/api/v1/matches/submit'))
            .toBe('http://analytics.internal/api/v1/matches/submit');
        expect(resolveHalUrl('https://svc.gcp.example/submit'))
            .toBe('https://svc.gcp.example/submit');
    });

    it('GIVEN a service-relative href THEN prefixed with the roster service base', () => {
        const out = resolveHalUrl('/api/v1/rosters/r-101/lock');
        expect(out.endsWith('/api/v1/rosters/r-101/lock')).toBe(true);
        expect(out).not.toBe('/api/v1/rosters/r-101/lock'); // something was prefixed
    });
});
