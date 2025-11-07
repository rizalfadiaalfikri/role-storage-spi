# Docker Setup untuk Keycloak Role Storage SPI

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+

## Quick Start

### Development Mode

1. **Build dan jalankan semua services:**
   ```bash
   docker-compose up --build
   ```

2. **Atau jalankan di background:**
   ```bash
   docker-compose up -d --build
   ```

3. **Akses Keycloak:**
   - URL: http://localhost:8080
   - Admin Console: http://localhost:8080/admin
   - Default admin credentials akan ditampilkan di console saat pertama kali start

4. **Stop services:**
   ```bash
   docker-compose down
   ```

5. **Stop dan hapus volumes (data akan dihapus):**
   ```bash
   docker-compose down -v
   ```

### Production Mode

1. **Buat file `.env` untuk environment variables:**
   ```bash
   MYSQL_ROOT_PASSWORD=your_secure_root_password
   MYSQL_PASSWORD=your_secure_keycloak_password
   CUSTOM_DB_ROOT_PASSWORD=your_secure_custom_db_password
   CUSTOM_DB_NAME=user_store
   CUSTOM_DB_USER=root
   CUSTOM_DB_PASSWORD=your_secure_password
   ```

2. **Jalankan dengan production compose:**
   ```bash
   docker-compose -f docker-compose.prod.yml --env-file .env up -d --build
   ```

## Services

### 1. MySQL Keycloak (`mysql-keycloak`)
- Port: `3306`
- Database: `keycloak`
- User: `keycloak` / Password: `keycloak`
- Digunakan untuk Keycloak internal database

### 2. MySQL Custom Roles (`mysql-custom-roles`)
- Port: `3307` (mapped dari internal 3306)
- Database: `user_store`
- User: `root` / Password: (kosong)
- Digunakan untuk custom role storage

### 3. Keycloak (`keycloak`)
- Port: `8080` (HTTP), `8443` (HTTPS)
- Custom provider sudah terinstall
- Configuration via system properties atau Admin Console

## Konfigurasi Custom Role Storage Provider

### Opsi 1: Via Keycloak Admin Console (Recommended)

1. Login ke Admin Console: http://localhost:8080/admin
2. Pilih Realm → Components → Add Provider
3. Pilih "Simple Custom Role Storage Provider with MySQL Database"
4. Isi konfigurasi:
   - **Database URL**: `jdbc:mysql://mysql-custom-roles:3306/user_store?useSSL=false&serverTimezone=UTC`
   - **Database Username**: `root`
   - **Database Password**: (kosong atau sesuai konfigurasi)
   - **Database Driver**: `com.mysql.cj.jdbc.Driver`

### Opsi 2: Via System Properties (Fallback)

System properties sudah dikonfigurasi di `docker-compose.yml`:
```yaml
JAVA_OPTS_APPEND: >-
  -Dquarkus.datasource.user-store.jdbc.url=jdbc:mysql://mysql-custom-roles:3306/user_store?useSSL=false&serverTimezone=UTC
  -Dquarkus.datasource.user-store.username=root
  -Dquarkus.datasource.user-store.password=
  -Dquarkus.datasource.user-store.db-kind=mysql
```

### Opsi 3: Via Environment Variables

Tambahkan di `docker-compose.yml`:
```yaml
environment:
  DB_URL: jdbc:mysql://mysql-custom-roles:3306/user_store
  DB_USERNAME: root
  DB_PASSWORD: ""
  DB_DRIVER: com.mysql.cj.jdbc.Driver
```

## Database Access

### Connect ke MySQL Keycloak:
```bash
docker exec -it keycloak-mysql mysql -ukeycloak -pkeycloak keycloak
```

### Connect ke MySQL Custom Roles:
```bash
docker exec -it mysql-custom-roles mysql -uroot user_store
```

### Atau dari host machine:
```bash
# Keycloak DB
mysql -h localhost -P 3306 -ukeycloak -pkeycloak keycloak

# Custom Roles DB
mysql -h localhost -P 3307 -uroot user_store
```

## Troubleshooting

### 1. Keycloak tidak bisa connect ke database
- Pastikan MySQL sudah healthy: `docker-compose ps`
- Check logs: `docker-compose logs mysql-keycloak`
- Pastikan network sudah terhubung: `docker network inspect keycloak-network`

### 2. Custom provider tidak muncul
- Check apakah JAR file sudah ter-copy: `docker exec keycloak ls -la /opt/keycloak/providers/`
- Restart Keycloak: `docker-compose restart keycloak`
- Check logs: `docker-compose logs keycloak`

### 3. Port sudah digunakan
- Ubah port mapping di `docker-compose.yml`:
  ```yaml
  ports:
    - "8081:8080"  # Ubah 8080 menjadi 8081
  ```

### 4. Database connection error
- Pastikan MySQL sudah running: `docker-compose ps`
- Check connection string di Admin Console
- Verify network connectivity: `docker exec keycloak ping mysql-custom-roles`

## Useful Commands

```bash
# View logs
docker-compose logs -f keycloak
docker-compose logs -f mysql-custom-roles

# Rebuild setelah code changes
docker-compose build keycloak
docker-compose up -d keycloak

# Access Keycloak container
docker exec -it keycloak-with-custom-provider /bin/bash

# Check provider installation
docker exec keycloak ls -la /opt/keycloak/providers/

# Backup database
docker exec mysql-custom-roles mysqldump -uroot user_store > backup.sql

# Restore database
docker exec -i mysql-custom-roles mysql -uroot user_store < backup.sql
```

## Volumes

Data disimpan di Docker volumes:
- `mysql-keycloak-data`: Data Keycloak database
- `mysql-custom-roles-data`: Data custom roles database
- `keycloak-data`: Keycloak server data

Untuk backup, gunakan:
```bash
docker run --rm -v mysql-custom-roles-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-custom-roles-backup.tar.gz /data
```

## Production Considerations

1. **Security:**
   - Ganti semua default passwords
   - Gunakan secrets management (Docker secrets, Vault, etc.)
   - Enable HTTPS/TLS
   - Restrict network access

2. **Performance:**
   - Tune MySQL configuration
   - Adjust JVM heap size
   - Use connection pooling
   - Enable caching

3. **Monitoring:**
   - Enable health checks
   - Setup metrics collection
   - Configure logging
   - Use monitoring tools (Prometheus, Grafana)

4. **Backup:**
   - Setup automated database backups
   - Backup Keycloak configuration
   - Test restore procedures



