-- Script untuk setup database PostgreSQL untuk Keycloak Role Storage SPI
-- Jalankan script ini sebagai user postgres atau user dengan privilege CREATE DATABASE

-- 1. Database untuk Keycloak internal
-- Database ini digunakan oleh Keycloak untuk menyimpan konfigurasi, users, clients, dll
CREATE DATABASE keycloak;

-- 2. Database untuk Custom Role Storage
-- Database ini digunakan oleh custom role storage provider untuk menyimpan custom roles
CREATE DATABASE user_store;

-- Grant privileges (jika diperlukan)
-- Pastikan user postgres memiliki akses ke kedua database
-- Default: user postgres sudah memiliki akses penuh

-- Catatan:
-- - Username: postgres
-- - Password: root (sesuai permintaan)
-- - Port: 5432 (default PostgreSQL)
-- - Host: localhost (dari host machine) atau host.docker.internal (dari Docker container)

