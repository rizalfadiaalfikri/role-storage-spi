# PowerShell script untuk membuat Role Storage Provider component via Admin REST API

$KeycloakUrl = "http://localhost:8080"
$Realm = "master"
$AdminUser = "admin"
$AdminPassword = "admin"

Write-Host "Getting admin access token..."

$TokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        username = $AdminUser
        password = $AdminPassword
        grant_type = "password"
        client_id = "admin-cli"
    }

$Token = $TokenResponse.access_token

if (-not $Token) {
    Write-Host "Failed to get access token. Please check Keycloak is running and credentials are correct." -ForegroundColor Red
    exit 1
}

Write-Host "Token obtained successfully" -ForegroundColor Green

# Check if component already exists
Write-Host "Checking if component already exists..."
$Headers = @{
    Authorization = "Bearer $Token"
    "Content-Type" = "application/json"
}

$ExistingComponents = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/components?parent=$Realm&type=org.keycloak.storage.role.RoleStorageProvider" `
    -Method Get `
    -Headers $Headers

$Existing = $ExistingComponents | Where-Object { $_.providerId -eq "simple-mysql-role-storage" }

if ($Existing) {
    Write-Host "Component already exists with ID: $($Existing.id)" -ForegroundColor Yellow
    Write-Host "Deleting existing component..."
    Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/components/$($Existing.id)" `
        -Method Delete `
        -Headers $Headers
    Write-Host "Existing component deleted" -ForegroundColor Green
}

# Create new component
Write-Host "Creating Role Storage Provider component..."

$ComponentData = @{
    name = "simple-mysql-role-storage"
    providerId = "simple-mysql-role-storage"
    providerType = "org.keycloak.storage.role.RoleStorageProvider"
    parentId = $Realm
    config = @{
        dbUrl = @("jdbc:postgresql://host.docker.internal:5432/user_store")
        dbUsername = @("postgres")
        dbPassword = @("root")
        dbDriver = @("org.postgresql.Driver")
    }
} | ConvertTo-Json -Depth 10

try {
    $Response = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/components" `
        -Method Post `
        -Headers $Headers `
        -Body $ComponentData
    
    Write-Host "Component created successfully!" -ForegroundColor Green
    Write-Host "Response: $($Response | ConvertTo-Json)"
} catch {
    Write-Host "Failed to create component." -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Component created. Please restart Keycloak or wait for it to reload." -ForegroundColor Green
Write-Host "After restart, check logs for: 'Creating Simple CustomRoleStorageProvider with MySQL storage'" -ForegroundColor Yellow

