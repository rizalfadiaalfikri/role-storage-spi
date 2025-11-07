# Testing Custom Role Storage Provider

## Masalah: Roles tidak muncul di Keycloak

Setelah membuat component via API, roles dari database tidak muncul. Berikut langkah troubleshooting:

## Step 1: Restart Keycloak

Setelah membuat component via API, **WAJIB restart Keycloak** agar component dimuat:

```bash
docker-compose restart keycloak
```

Atau:

```bash
docker-compose down
docker-compose up
```

## Step 2: Verifikasi Component Terdaftar

Cek apakah component sudah terdaftar:

```bash
# Get token
TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

# List components
curl -X GET "http://localhost:8080/admin/realms/master/components?type=org.keycloak.storage.role.RoleStorageProvider" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'
```

## Step 3: Cek Logs

Setelah restart, cek logs untuk melihat apakah provider dipanggil:

```bash
docker-compose logs -f keycloak
```

Cari log:
- `Creating Simple CustomRoleStorageProvider with MySQL storage`
- `Searching for realm roles`
- `Found X roles from database`

## Step 4: Test dengan Search

Di Keycloak Admin Console:
1. Buka "Realm roles"
2. **Gunakan search box** dan ketik nama role dari database (misalnya: "admin", "user", "manager")
3. Roles seharusnya muncul

## Step 5: Verifikasi Data di Database

Pastikan data di database benar:

```sql
SELECT id, name, description, realm_id, client_id 
FROM custom_roles 
WHERE realm_id = 'master' 
  AND client_id IS NULL;
```

Pastikan:
- `realm_id` = `'master'` (atau sesuai realm yang digunakan)
- `client_id` = `NULL` untuk realm roles
- Data ada di database

## Troubleshooting

### Jika provider tidak dipanggil:

1. **Pastikan component enabled:**
   ```bash
   # Get component ID
   COMPONENT_ID=$(curl -s -X GET "http://localhost:8080/admin/realms/master/components?type=org.keycloak.storage.role.RoleStorageProvider" \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" | jq -r '.[] | select(.providerId=="simple-mysql-role-storage") | .id')
   
   # Check component details
   curl -X GET "http://localhost:8080/admin/realms/master/components/$COMPONENT_ID" \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" | jq '.'
   ```

2. **Pastikan database connection berhasil:**
   - Cek logs untuk error database connection
   - Pastikan PostgreSQL running dan accessible dari Docker

3. **Test provider langsung:**
   - Gunakan search di Keycloak Admin Console
   - Provider akan dipanggil saat search

### Catatan Penting:

- **Keycloak Role Storage Provider hanya dipanggil saat:**
  - Search roles (via search box)
  - Get role by name
  - Get role by ID
  
- **Provider TIDAK otomatis menampilkan semua roles di list view**
- **Harus menggunakan search untuk melihat roles dari provider**

