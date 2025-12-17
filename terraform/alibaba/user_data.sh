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
REDIS_PORT=6379
REDIS_PASSWORD=${redis_password}
EOF

# Login to Alibaba Container Registry
docker login --username=\$REGISTRY_USERNAME --password=\$REGISTRY_PASSWORD ${docker_registry}

# Pull images
docker pull ${docker_registry}/ingestion-service:latest
docker pull ${docker_registry}/analytics-service:latest
docker pull ${docker_registry}/websocket-api:latest
docker pull ${docker_registry}/dashboard:latest

# Create docker-compose.yml for external Redis
cat > docker-compose.yml <<'COMPOSE_EOF'
version: '3.8'

services:
  ingestion-service:
    image: ${docker_registry}/ingestion-service:latest
    container_name: finbot-ingestion
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  analytics-service:
    image: ${docker_registry}/analytics-service:latest
    container_name: finbot-analytics
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  websocket-api:
    image: ${docker_registry}/websocket-api:latest
    container_name: finbot-websocket-api
    ports:
      - "8080:8080"
    env_file: .env
    restart: unless-stopped
    networks:
      - finbot-network

  dashboard:
    image: ${docker_registry}/dashboard:latest
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

echo "Finbot deployment completed successfully"
