#!/bin/bash
set -e

echo "=========================================="
echo "Building Finbot - All Services"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building shared domain...${NC}"
mvn -f shared-domain/pom.xml clean install -DskipTests

echo -e "${BLUE}Building ingestion service...${NC}"
mvn -f ingestion-service/pom.xml clean package -DskipTests

echo -e "${BLUE}Building analytics service...${NC}"
mvn -f analytics-service/pom.xml clean package -DskipTests

echo -e "${BLUE}Building websocket API...${NC}"
mvn -f websocket-api/pom.xml clean package -DskipTests

echo -e "${BLUE}Building dashboard...${NC}"
cd dashboard
npm install
npm run build
cd ..

echo -e "${GREEN}=========================================="
echo -e "Build completed successfully!"
echo -e "==========================================${NC}"
