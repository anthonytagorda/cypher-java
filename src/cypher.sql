DROP DATABASE IF EXISTS cypher;
CREATE DATABASE IF NOT EXISTS cypher;
USE cypher;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE players;
ALTER TABLE players AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- PLAYERS / AUTH
-- =========================
CREATE TABLE IF NOT EXISTS players (
    player_id    INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) NOT NULL UNIQUE,
    password     VARCHAR(100) NOT NULL,
    status       ENUM('online','offline','in-game') NOT NULL DEFAULT 'offline',
    is_banned     TINYINT(1) NOT NULL DEFAULT 0,
    total_wins    INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- GAMES
-- =========================
CREATE TABLE IF NOT EXISTS games (
    game_id          INT AUTO_INCREMENT PRIMARY KEY,
    host_player_id   INT NOT NULL,
    status           ENUM('WAITING','IN_PROGRESS','ENDED','CANCELLED') NOT NULL DEFAULT 'WAITING',
    started_at       DATETIME NULL,
    ended_at         DATETIME NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_games_host
    FOREIGN KEY (host_player_id) REFERENCES players(player_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    );

-- players inside a game
CREATE TABLE IF NOT EXISTS game_players (
    game_id          INT NOT NULL,
    player_id        INT NOT NULL,
    joined_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_host          TINYINT(1) NOT NULL DEFAULT 0,
    total_score      INT NOT NULL DEFAULT 0,
    round_wins       INT NOT NULL DEFAULT 0,
    PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_game_players_game
        FOREIGN KEY (game_id) REFERENCES games(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_game_players_player
        FOREIGN KEY (player_id) REFERENCES players(player_id)
        ON DELETE CASCADE ON UPDATE CASCADE
    );

-- =========================
-- ROUNDS
-- =========================
CREATE TABLE IF NOT EXISTS rounds (
    round_id         INT AUTO_INCREMENT PRIMARY KEY,
    game_id          INT NOT NULL,
    round_number     INT NOT NULL,
    letters          VARCHAR(64) NOT NULL,
    started_at       DATETIME NOT NULL,
    ended_at         DATETIME NULL,
    winner_player_id INT NULL,
    CONSTRAINT uq_round UNIQUE (game_id, round_number),
    CONSTRAINT fk_rounds_game
        FOREIGN KEY (game_id) REFERENCES games(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rounds_winner
        FOREIGN KEY (winner_player_id) REFERENCES players(player_id)
        ON DELETE SET NULL ON UPDATE CASCADE
    );

-- =========================
-- WORD SUBMISSIONS
-- =========================
CREATE TABLE IF NOT EXISTS word_submissions (
    submission_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id          INT NOT NULL,
    round_id         INT NOT NULL,
    player_id        INT NOT NULL,
    word             VARCHAR(64) NOT NULL,
    is_valid         TINYINT(1) NOT NULL,
    points           INT NOT NULL DEFAULT 0,
    submitted_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ws_game
        FOREIGN KEY (game_id) REFERENCES games(game_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ws_round
        FOREIGN KEY (round_id) REFERENCES rounds(round_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ws_player
        FOREIGN KEY (player_id) REFERENCES players(player_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_ws_game_round (game_id, round_id),
    INDEX idx_ws_player (player_id),
    INDEX idx_ws_word (word)
    );

-- =========================
-- LEADERBOARD (GLOBAL STATS)
-- =========================
CREATE TABLE IF NOT EXISTS leaderboard (
    player_id         INT PRIMARY KEY,
    games_played      INT NOT NULL DEFAULT 0,
    games_won         INT NOT NULL DEFAULT 0,
    total_score       INT NOT NULL DEFAULT 0,
    best_single_round INT NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leaderboard_player
        FOREIGN KEY (player_id) REFERENCES players(player_id)
        ON DELETE CASCADE ON UPDATE CASCADE
    );

-- =========================
-- SETTINGS
-- =========================
CREATE TABLE settings (
    id                INT PRIMARY KEY,
    waiting_time_sec  INT NOT NULL DEFAULT 30,
    game_duration_sec INT NOT NULL DEFAULT 180
);

INSERT INTO settings (id, waiting_time_sec, game_duration_sec)
VALUES (1, 30, 180);
