#!/bin/bash
set -e

# Update system
apt-get update
apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
systemctl enable docker
systemctl start docker

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install gcloud CLI
echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -
apt-get update && apt-get install -y google-cloud-cli

# Configure Docker for Artifact Registry
gcloud auth configure-docker ${region}-docker.pkg.dev

# Create application directory
mkdir -p /opt/finbot
cd /opt/finbot

# Create .env file
cat > .env <<EOF
POLYGON_API_KEY=${polygon_api_key}
POLYGON_WEBSOCKET_URL=wss://socket.polygon.io/stocks
POLYGON_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
ANALYTICS_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
SNAPSHOT_INTERVAL=5s
BROADCAST_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
BROADCAST_INTERVAL=1s
REDIS_HOST=${redis_host}
REDIS_PORT=${redis_port}
EOF

# Pull images from Artifact Registry
docker pull ${region}-docker.pkg.dev/${project_id}/finbot/ingestion-service:latest
docker pull ${region}-docker.pkg.dev/${project_id}/finbot/analytics-service:latest
docker pull ${region}-docker.pkg.dev/${project_id}/finbot/websocket-api:latest
docker pull ${region}-docker.pkg.dev/${project_id}/finbot/dashboard:latest

# Create docker-compose.yml for external Redis
cat > docker-compose.yml <<'COMPOSE_EOF'
version: '3.8'

services:
  ingestion-service:
    image: ${region}-docker.pkg.dev/${project_id}/finbot/ingestion-service:latest
    container_name: finbot-ingestion
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  analytics-service:
    image: ${region}-docker.pkg.dev/${project_id}/finbot/analytics-service:latest
    container_name: finbot-analytics
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  websocket-api:
    image: ${region}-docker.pkg.dev/${project_id}/finbot/websocket-api:latest
    container_name: finbot-websocket-api
    ports:
      - "8080:8080"
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  dashboard:
    image: ${region}-docker.pkg.dev/${project_id}/finbot/dashboard:latest
    container_name: finbot-dashboard
    ports:
      - "3000:80"
    restart: unless-stopped
    networks:
      - finbot-network

networks:
  finbot-network:
    driver: bridge
COMPOSE_EOF

# Start services
docker-compose up -d

# Setup log rotation
cat > /etc/logrotate.d/finbot <<EOF
/var/lib/docker/containers/*/*.log {
  rotate 7
  daily
  compress
  missingok
  delaycompress
  copytruncate
}
EOF

# Setup monitoring
apt-get install -y google-cloud-ops-agent

echo "Finbot deployment completed successfully"
