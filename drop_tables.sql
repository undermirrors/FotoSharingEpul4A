USE fotoshareDB;

-- Supprimer les données existantes
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE commentaire;
TRUNCATE TABLE partage;
TRUNCATE TABLE album_photo;
TRUNCATE TABLE photo;
TRUNCATE TABLE album;
TRUNCATE TABLE utilisateur;
SET FOREIGN_KEY_CHECKS = 1;
