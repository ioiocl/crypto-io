#!/bin/bash

echo "🔍 Finbot Data Flow Diagnostic"
echo "================================"
echo ""

echo "1️⃣ Checking if ticks are arriving..."
docker compose logs --tail=5 ingestion-service | grep "Processed Binance ticker"
echo ""

echo "2️⃣ Checking if analytics is processing ticks..."
docker compose logs --tail=5 analytics-service | grep "Processed tick"
echo ""

echo "3️⃣ Checking if snapshots are being saved..."
docker compose logs --tail=10 analytics-service | grep "Saved snapshot"
echo ""

echo "4️⃣ Checking Redis for snapshots..."
docker exec finbot-redis redis-cli KEYS "latest_snapshot:*"
echo ""

echo "5️⃣ Checking BTC snapshot content..."
docker exec finbot-redis redis-cli GET "latest_snapshot:BTC" | head -c 200
echo "..."
echo ""

echo "6️⃣ Checking WebSocket connections..."
docker compose logs --tail=5 websocket-api | grep "Client connected"
echo ""

echo "7️⃣ Checking if WebSocket is broadcasting..."
docker compose logs --tail=5 websocket-api | grep -i "broadcast"
echo ""

echo "✅ Diagnostic complete!"
