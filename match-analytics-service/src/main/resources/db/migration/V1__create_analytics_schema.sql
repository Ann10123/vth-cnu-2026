CREATE TABLE matches (
    id VARCHAR(36) PRIMARY KEY,
    roster_id VARCHAR(36) NOT NULL,
    map_name VARCHAR(30) NOT NULL,
    result VARCHAR(10) NOT NULL,
    round_margin INT NOT NULL,
    played_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE match_players (
    id VARCHAR(36) PRIMARY KEY,
    match_id VARCHAR(36) NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id VARCHAR(36) NOT NULL,
    kills INT NOT NULL,
    deaths INT NOT NULL,
    assists INT NOT NULL,
    combat_score INT NOT NULL,
    delta_rr INT NOT NULL,
    tier_status VARCHAR(20) NOT NULL
);
