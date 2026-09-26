CREATE DATABASE IF NOT EXISTS bantau
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bantau;

/* ------------------------------------------------------------------
   BANG players - TAI KHOAN NGUOI CHOI

   password_hash: chuoi hex 64 ky tu = SHA-256(salt + mat khau)
   salt:          chuoi hex 32 ky tu, sinh ngau nhien rieng cho tung nguoi

   KHONG BAO GIO luu mat khau tho. Salt rieng cho tung nguoi khien hai
   nguoi dat cung mot mat khau van ra hai chuoi bam khac nhau, nen ke
   tan cong khong the dung bang tra san (rainbow table).
   ------------------------------------------------------------------ */
CREATE TABLE IF NOT EXISTS players (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(16)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    salt          CHAR(32)     NOT NULL,
    wins          INT          NOT NULL DEFAULT 0,
    losses        INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* ------------------------------------------------------------------
   BANG matches - LICH SU TUNG TRAN DAU

   winner NULL nghia la tran chua phan thang bai (vi du server tat giua chung).
   reason: ALL_SUNK (ban chim het tau) hoac OPPONENT_LEFT (doi thu bo di).
   shots:  tong so phat ban hop le cua ca hai ben trong tran.
   ------------------------------------------------------------------ */
CREATE TABLE IF NOT EXISTS matches (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    player1    VARCHAR(16) NOT NULL,
    player2    VARCHAR(16) NOT NULL,
    winner     VARCHAR(16) NULL,
    reason     VARCHAR(32) NOT NULL,
    shots      INT         NOT NULL DEFAULT 0,
    started_at DATETIME    NOT NULL,
    ended_at   DATETIME    NOT NULL,
    INDEX ix_matches_player1 (player1),
    INDEX ix_matches_player2 (player2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/* ------------------------------------------------------------------
   Kiem tra nhanh sau khi tao xong - phai thay hai dong, so_dong = 0
   ------------------------------------------------------------------ */
SELECT 'players' AS bang, COUNT(*) AS so_dong FROM players
UNION ALL
SELECT 'matches', COUNT(*) FROM matches;
