-- Script untuk reset database Keycloak
-- Hapus dan recreate database keycloak untuk mengatasi Liquibase checksum error

-- Connect sebagai postgres user
-- psql -U postgres

-- Drop database jika ada
DROP DATABASE IF EXISTS keycloak;

-- Recreate database
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE keycloak TO postgres;

