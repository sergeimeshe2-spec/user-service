#!/bin/bash
# Microcks Auto-Sync Script
# This script runs every 10 minutes to check for spec changes and update Microcks

set -e

# Configuration
GITHUB_REPO="https://github.com/sergeimeshe2-spec/user-service.git"
LOCAL_REPO="/tmp/user-service-specs"
MICROCKS_URL="http://microcks:8080"
SPECS_DIR="$LOCAL_REPO/src/main/resources/specs"
LAST_HASH_FILE="/tmp/user-service-specs-last-hash"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting Microcks sync check..."

# Clone or update repository
if [ -d "$LOCAL_REPO" ]; then
    log "Updating local repository..."
    cd "$LOCAL_REPO"
    git fetch origin
    git reset --hard origin/main
else
    log "Cloning repository..."
    git clone -q --depth 1 "$GITHUB_REPO" "$LOCAL_REPO"
    cd "$LOCAL_REPO"
fi

# Get current hash of specs
CURRENT_HASH=$(find "$SPECS_DIR" -type f -name "*.yaml" -exec cat {} \; | md5sum | cut -d' ' -f1)

# Check if specs changed
if [ -f "$LAST_HASH_FILE" ]; then
    LAST_HASH=$(cat "$LAST_HASH_FILE")
    if [ "$CURRENT_HASH" = "$LAST_HASH" ]; then
        log "No changes detected in specifications"
        exit 0
    fi
fi

log "Changes detected! Updating Microcks..."

# Wait for Microcks to be ready
until curl -sf "$MICROCKS_URL/api/health" > /dev/null; do
    log "Waiting for Microcks to be ready..."
    sleep 5
done

# Import OpenAPI spec
log "Importing OpenAPI specification..."
curl -s -X POST "$MICROCKS_URL/api/artifacts/resource" \
    -F "file=@$SPECS_DIR/user-service-openapi.yaml" \
    -F "mainArtifact=true" || log "Failed to import OpenAPI spec"

# Import AsyncAPI spec
log "Importing AsyncAPI specification..."
curl -s -X POST "$MICROCKS_URL/api/artifacts/resource" \
    -F "file=@$SPECS_DIR/user-events-asyncapi.yaml" \
    -F "mainArtifact=true" || log "Failed to import AsyncAPI spec"

# Save current hash
echo "$CURRENT_HASH" > "$LAST_HASH_FILE"

log "Microcks sync completed successfully!"
