# Script untuk test Custom Role Storage Provider

Write-Host "=== Testing Custom Role Storage Provider ===" -ForegroundColor Yellow
Write-Host ""

# Step 1: Monitor logs in background
Write-Host "1. Starting log monitoring..." -ForegroundColor Cyan
Write-Host "   (This will show logs when provider is called)" -ForegroundColor Gray
Write-Host ""

# Step 2: Instructions
Write-Host "2. Test Provider:" -ForegroundColor Cyan
Write-Host "   a. Open Keycloak Admin Console: http://localhost:8080" -ForegroundColor White
Write-Host "   b. Login with admin/admin" -ForegroundColor White
Write-Host "   c. Go to: Realm roles" -ForegroundColor White
Write-Host "   d. Use the SEARCH box to search for roles" -ForegroundColor White
Write-Host "      (Try searching for role names from your database)" -ForegroundColor Gray
Write-Host ""

# Step 3: Check logs
Write-Host "3. After searching, check logs with:" -ForegroundColor Cyan
Write-Host "   docker-compose logs keycloak 2>&1 | Select-String -Pattern 'Creating Simple|Searching for realm roles|Found.*roles from database'" -ForegroundColor White
Write-Host ""

# Step 4: Verify data in database
Write-Host "4. Make sure you have data in database:" -ForegroundColor Cyan
Write-Host "   SELECT * FROM custom_roles WHERE realm_id = 'master' AND client_id IS NULL;" -ForegroundColor White
Write-Host ""

Write-Host "=== Expected Log Messages ===" -ForegroundColor Yellow
Write-Host "When you search for roles, you should see:" -ForegroundColor Gray
Write-Host "  - 'Creating Simple CustomRoleStorageProvider with MySQL storage'" -ForegroundColor Green
Write-Host "  - 'Searching for realm roles with query: ...'" -ForegroundColor Green
Write-Host "  - 'Found X roles from database'" -ForegroundColor Green
Write-Host ""

Write-Host "Press any key to continue monitoring logs..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Monitor logs
Write-Host "`n=== Monitoring Logs (Press Ctrl+C to stop) ===" -ForegroundColor Yellow
docker-compose logs -f keycloak 2>&1 | Select-String -Pattern "CustomRoleStorageProvider|Searching for realm roles|Found.*roles from database|Creating Simple"


