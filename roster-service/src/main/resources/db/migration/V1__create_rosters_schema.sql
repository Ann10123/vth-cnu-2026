CREATE TABLE players (
    player_id VARCHAR(36) PRIMARY KEY,
    player_tag VARCHAR(50) NOT NULL UNIQUE,
    role_type VARCHAR(20) NOT NULL DEFAULT 'PLAYER'
);

CREATE TABLE rosters (
    id VARCHAR(36) PRIMARY KEY,
    captain_id VARCHAR(36) NOT NULL REFERENCES players(player_id),
    map_name VARCHAR(30) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roster_members (
    id VARCHAR(36) PRIMARY KEY,
    roster_id VARCHAR(36) NOT NULL REFERENCES rosters(id) ON DELETE CASCADE,
    slot INT NOT NULL CHECK (slot >= 1 AND slot <= 5),
    player_id VARCHAR(36) NOT NULL REFERENCES players(player_id),
    agent_key VARCHAR(30) NOT NULL,
    role_type VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    CONSTRAINT unique_slot_per_roster UNIQUE (roster_id, slot),
    CONSTRAINT unique_agent_per_roster UNIQUE (roster_id, agent_key),
    CONSTRAINT unique_player_per_roster UNIQUE (roster_id, player_id)
);
