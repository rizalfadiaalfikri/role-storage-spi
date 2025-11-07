#!/bin/bash

# Script untuk membuat Role Storage Provider component via Admin REST API

KEYCLOAK_URL="http://localhost:8080"
REALM="master"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

echo "Getting admin access token..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "Failed to get access token. Please check Keycloak is running and credentials are correct."
    exit 1
fi

echo "Token obtained successfully"

# Check if component already exists
echo "Checking if component already exists..."
EXISTING=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM}/components?parent=${REALM}&type=org.keycloak.storage.role.RoleStorageProvider" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" | jq -r '.[] | select(.providerId=="simple-mysql-role-storage") | .id')

if [ ! -z "$EXISTING" ]; then
    echo "Component already exists with ID: $EXISTING"
    echo "Deleting existing component..."
    curl -s -X DELETE "${KEYCLOAK_URL}/admin/realms/${REALM}/components/${EXISTING}" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "Content-Type: application/json"
    echo "Existing component deleted"
fi

# Create new component
echo "Creating Role Storage Provider component..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/components" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "simple-mysql-role-storage",
    "providerId": "simple-mysql-role-storage",
    "providerType": "org.keycloak.storage.role.RoleStorageProvider",
    "parentId": "'"${REALM}"'",
    "config": {
      "dbUrl": ["jdbc:postgresql://host.docker.internal:5432/user_store"],
      "dbUsername": ["postgres"],
      "dbPassword": ["root"],
      "dbDriver": ["org.postgresql.Driver"]
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "200" ]; then
    echo "Component created successfully!"
    echo "Response: $BODY"
else
    echo "Failed to create component. HTTP Code: $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi

echo ""
echo "Component created. Please restart Keycloak or wait for it to reload."
echo "After restart, check logs for: 'Creating Simple CustomRoleStorageProvider with MySQL storage'"

