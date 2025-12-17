#!/bin/bash
set -e

# Check if registry URL is provided
if [ -z "$1" ]; then
    echo "Usage: ./docker-build-push.sh <registry-url> [tag]"
    echo "Example: ./docker-build-push.sh registry.us-west-1.aliyuncs.com/finbot latest"
    exit 1
fi

REGISTRY=$1
TAG=${2:-latest}

echo "=========================================="
echo "Building and Pushing Docker Images"
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo "=========================================="

# Build and push ingestion service
echo "Building ingestion-service..."
docker build -t $REGISTRY/ingestion-service:$TAG -f ingestion-service/Dockerfile .
echo "Pushing ingestion-service..."
docker push $REGISTRY/ingestion-service:$TAG

# Build and push analytics service
echo "Building analytics-service..."
docker build -t $REGISTRY/analytics-service:$TAG -f analytics-service/Dockerfile .
echo "Pushing analytics-service..."
docker push $REGISTRY/analytics-service:$TAG

# Build and push websocket API
echo "Building websocket-api..."
docker build -t $REGISTRY/websocket-api:$TAG -f websocket-api/Dockerfile .
echo "Pushing websocket-api..."
docker push $REGISTRY/websocket-api:$TAG

# Build and push dashboard
echo "Building dashboard..."
docker build -t $REGISTRY/dashboard:$TAG -f dashboard/Dockerfile ./dashboard
echo "Pushing dashboard..."
docker push $REGISTRY/dashboard:$TAG

echo "=========================================="
echo "All images built and pushed successfully!"
echo "=========================================="
