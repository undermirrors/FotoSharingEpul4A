CREATE DATABASE IF NOT EXISTS fotoshareDB;

USE fotoshareDB;
-- 1. Table Utilisateur (Ajout de 'enabled')
CREATE TABLE IF NOT EXISTS utilisateur
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM ('USER', 'ADMIN', 'MODERATOR') DEFAULT 'USER',
    enabled       BOOLEAN                             DEFAULT TRUE,
    created_at    TIMESTAMP                           DEFAULT CURRENT_TIMESTAMP
);
-- 2. Table Photo (Ajout de 'storage_filename' et 'content_type')
CREATE TABLE IF NOT EXISTS photo
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(100) NOT NULL,
    description       TEXT,
    original_filename VARCHAR(255),                 -- Nom d'origine pour affichage
    storage_filename  VARCHAR(255) NOT NULL UNIQUE, -- UUID sur le disque
    content_type      VARCHAR(50)  NOT NULL,        -- ex: image/jpeg
    visibility        ENUM ('PRIVATE', 'PUBLIC') DEFAULT 'PRIVATE',
    owner_id          BIGINT       NOT NULL,
    created_at        TIMESTAMP                  DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    INDEX idx_photo_owner (owner_id)
);
-- 3. Table Album
CREATE TABLE IF NOT EXISTS album
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id    BIGINT       NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES utilisateur (id) ON DELETE CASCADE
);
-- 4. Table de liaison Album-Photo
CREATE TABLE IF NOT EXISTS album_photo
(
    album_id BIGINT NOT NULL,
    photo_id BIGINT NOT NULL,
    PRIMARY KEY (album_id, photo_id),
    FOREIGN KEY (album_id) REFERENCES album (id) ON DELETE CASCADE,
    FOREIGN KEY (photo_id) REFERENCES photo (id) ON DELETE CASCADE
);
-- 5. Table Partage (Ajout Contrainte d'Unicité)
CREATE TABLE IF NOT EXISTS partage
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    photo_id         BIGINT NOT NULL,
    user_id          BIGINT NOT NULL,
    permission_level ENUM ('READ', 'COMMENT', 'ADMIN') DEFAULT 'READ',
    created_at       TIMESTAMP                         DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (photo_id) REFERENCES photo (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    UNIQUE KEY uk_photo_user (photo_id, user_id), -- Un seul partage par couple photo/user
    INDEX idx_partage_user (user_id)
);
-- 6. Table Commentaire
CREATE TABLE IF NOT EXISTS commentaire
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    text       TEXT   NOT NULL,
    photo_id   BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (photo_id) REFERENCES photo (id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES utilisateur (id) ON DELETE CASCADE
);