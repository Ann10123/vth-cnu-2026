INSERT INTO players (player_id, player_tag, role_type) VALUES
    ('p-001', 'ProGamer#EUW', 'CAPTAIN'),
    ('p-002', 'Tactician#UA', 'PLAYER'),
    ('p-003', 'DuelistKing#NA', 'PLAYER'),
    ('p-004', 'SentinelMaster#KR', 'PLAYER'),
    ('p-005', 'SmokesGod#BR', 'PLAYER');

INSERT INTO rosters (id, captain_id, map_name, status) VALUES
    ('r-101', 'p-001', 'Ascent', 'DRAFT');

INSERT INTO roster_members (id, roster_id, slot, player_id, agent_key, role_type) VALUES
    ('m-001', 'r-101', 1, 'p-001', 'jett', 'CAPTAIN'),
    ('m-002', 'r-101', 2, 'p-002', 'omen', 'PLAYER');
