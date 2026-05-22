#!/bin/bash

# Secrets Manager Bootstrap Script
# This script generates unique cryptographic keys for local development and saves them to a .env file.

ENV_FILE=".env"

if [ -f "$ENV_FILE" ]; then
    echo "[INFO] .env file already exists. Skipping generation."
    exit 0
fi

echo "[INFO] Generating fresh cryptographic keys for local environment..."

# 1. Generate JWT EC P-256 Key Pair (PKCS#8 for private, X.509 for public)
# Requires OpenSSL
if command -v openssl >/dev/null 2>&1; then
    openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem
    openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -nocrypt -out private_key.der
    openssl ec -in private_key.pem -pubout -outform DER -out public_key.der
    
    JWT_PRIVATE_KEY=$(base64 < private_key.der | tr -d '\n')
    JWT_PUBLIC_KEY=$(base64 < public_key.der | tr -d '\n')
    
    # Cleanup temporary files
    rm private_key.pem private_key.der public_key.der
else
    echo "[ERROR] OpenSSL not found. Please install OpenSSL or manually provide JWT keys."
    exit 1
fi

# 2. Generate Master Key V1 (32 bytes for AES-256)
MASTER_KEY__V1=$(openssl rand -base64 32 | tr -d '\n')

# 3. Create .env file
cat <<EOF > "$ENV_FILE"
# Database Configuration
POSTGRES_DB=secrets_manager
POSTGRES_USER=sm_user
POSTGRES_PASSWORD=$(openssl rand -hex 12)

# JWT Authentication (EC P-256)
JWT_PRIVATE_KEY=$JWT_PRIVATE_KEY
JWT_PUBLIC_KEY=$JWT_PUBLIC_KEY

# Master Key Root of Trust (AES-256)
MASTER_KEY__V1=$MASTER_KEY__V1
MASTER_KEY_DEFAULT_ALGORITHM=AES-256-GCM

# Initial Admin Credentials
BOOTSTRAP_USERNAME=admin
BOOTSTRAP_PASSWORD=AdminPassword123!
EOF

chmod 600 "$ENV_FILE"
echo "[SUCCESS] .env file generated with unique keys. You can now run 'docker compose up --build'."
