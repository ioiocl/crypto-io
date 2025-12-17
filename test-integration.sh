#!/bin/bash

echo "🧪 Finbot Integration Test"
echo "============================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_passed=0
test_failed=0

# Test 1: Check if services are running
echo "Test 1: Services Running"
if docker compose ps | grep -q "Up"; then
    echo -e "${GREEN}✓ Services are running${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ Services are not running${NC}"
    ((test_failed++))
fi
echo ""

# Test 2: Check Binance connection
echo "Test 2: Binance Connection"
if docker compose logs ingestion-service | grep -q "Connected to Binance"; then
    echo -e "${GREEN}✓ Connected to Binance${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ Not connected to Binance${NC}"
    ((test_failed++))
fi
echo ""

# Test 3: Check if ticks are arriving
echo "Test 3: Receiving Ticks"
tick_count=$(docker compose logs --tail=100 ingestion-service | grep -c "Processed Binance ticker")
if [ "$tick_count" -gt 0 ]; then
    echo -e "${GREEN}✓ Receiving ticks ($tick_count in last 100 lines)${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ No ticks received${NC}"
    ((test_failed++))
fi
echo ""

# Test 4: Check if analytics is processing
echo "Test 4: Analytics Processing"
analytics_count=$(docker compose logs --tail=100 analytics-service | grep -c "Processed tick")
if [ "$analytics_count" -gt 0 ]; then
    echo -e "${GREEN}✓ Analytics processing ticks ($analytics_count in last 100 lines)${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ Analytics not processing${NC}"
    ((test_failed++))
fi
echo ""

# Test 5: Check if snapshots are being saved
echo "Test 5: Snapshots Saved to Redis"
snapshot_count=$(docker compose logs --tail=100 analytics-service | grep -c "Saved snapshot")
if [ "$snapshot_count" -gt 0 ]; then
    echo -e "${GREEN}✓ Snapshots being saved ($snapshot_count in last 100 lines)${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ Snapshots NOT being saved - THIS IS THE PROBLEM!${NC}"
    echo -e "${YELLOW}  → Analytics service needs to be rebuilt${NC}"
    ((test_failed++))
fi
echo ""

# Test 6: Check Redis has data
echo "Test 6: Redis Has Snapshot Data"
redis_keys=$(docker exec finbot-redis redis-cli KEYS "latest_snapshot:*" | wc -l)
if [ "$redis_keys" -gt 0 ]; then
    echo -e "${GREEN}✓ Redis has $redis_keys snapshots${NC}"
    docker exec finbot-redis redis-cli KEYS "latest_snapshot:*"
    ((test_passed++))
else
    echo -e "${RED}✗ Redis has NO snapshots${NC}"
    ((test_failed++))
fi
echo ""

# Test 7: Check BTC snapshot specifically
echo "Test 7: BTC Snapshot Exists"
if docker exec finbot-redis redis-cli EXISTS "latest_snapshot:BTC" | grep -q "1"; then
    echo -e "${GREEN}✓ BTC snapshot exists in Redis${NC}"
    echo "Sample data:"
    docker exec finbot-redis redis-cli GET "latest_snapshot:BTC" | head -c 150
    echo "..."
    ((test_passed++))
else
    echo -e "${RED}✗ BTC snapshot missing${NC}"
    ((test_failed++))
fi
echo ""

# Test 8: Check WebSocket API is running
echo "Test 8: WebSocket API Running"
if docker compose ps websocket-api | grep -q "Up"; then
    echo -e "${GREEN}✓ WebSocket API is running${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ WebSocket API is down${NC}"
    ((test_failed++))
fi
echo ""

# Test 9: Test WebSocket endpoint directly
echo "Test 9: WebSocket Endpoint Accessible"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/q/health | grep -q "200"; then
    echo -e "${GREEN}✓ WebSocket API health check passed${NC}"
    ((test_passed++))
else
    echo -e "${YELLOW}⚠ WebSocket API health check failed (might be normal)${NC}"
fi
echo ""

# Test 10: Check dashboard is accessible
echo "Test 10: Dashboard Accessible"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200"; then
    echo -e "${GREEN}✓ Dashboard is accessible${NC}"
    ((test_passed++))
else
    echo -e "${RED}✗ Dashboard is not accessible${NC}"
    ((test_failed++))
fi
echo ""

# Summary
echo "================================"
echo "📊 Test Results"
echo "================================"
echo -e "Passed: ${GREEN}$test_passed${NC}"
echo -e "Failed: ${RED}$test_failed${NC}"
echo ""

if [ "$test_failed" -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed! System should be working.${NC}"
    echo ""
    echo "If frontend still shows 'Waiting for data':"
    echo "1. Hard refresh browser (Ctrl+Shift+R)"
    echo "2. Check browser console for errors (F12)"
    echo "3. Verify WebSocket connection in Network tab"
else
    echo -e "${RED}❌ Some tests failed. Issues found:${NC}"
    echo ""
    
    if [ "$snapshot_count" -eq 0 ]; then
        echo -e "${YELLOW}🔧 CRITICAL: Snapshots not being saved!${NC}"
        echo "Fix: Run these commands:"
        echo "  docker compose build analytics-service"
        echo "  docker compose up -d analytics-service"
        echo "  docker compose logs -f analytics-service"
        echo ""
    fi
    
    if [ "$redis_keys" -eq 0 ]; then
        echo -e "${YELLOW}🔧 Redis has no data${NC}"
        echo "This means snapshots aren't being saved."
        echo ""
    fi
fi

echo ""
echo "🔍 Quick Debug Commands:"
echo "  docker compose logs -f analytics-service    # Watch analytics logs"
echo "  docker compose logs -f websocket-api        # Watch WebSocket logs"
echo "  docker exec -it finbot-redis redis-cli      # Connect to Redis"
echo ""
