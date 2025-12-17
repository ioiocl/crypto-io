# Finbot Integration Test
Write-Host "🧪 Finbot Integration Test" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host ""

$testPassed = 0
$testFailed = 0

# Test 1: Services Running
Write-Host "Test 1: Services Running" -ForegroundColor Yellow
$services = docker compose ps
if ($services -match "Up") {
    Write-Host "✓ Services are running" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ Services are not running" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 2: Binance Connection
Write-Host "Test 2: Binance Connection" -ForegroundColor Yellow
$binanceLogs = docker compose logs ingestion-service 2>&1 | Select-String "Connected to Binance"
if ($binanceLogs) {
    Write-Host "✓ Connected to Binance" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ Not connected to Binance" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 3: Receiving Ticks
Write-Host "Test 3: Receiving Ticks" -ForegroundColor Yellow
$ticks = docker compose logs --tail=100 ingestion-service 2>&1 | Select-String "Processed Binance ticker"
$tickCount = ($ticks | Measure-Object).Count
if ($tickCount -gt 0) {
    Write-Host "✓ Receiving ticks ($tickCount in last 100 lines)" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ No ticks received" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 4: Analytics Processing
Write-Host "Test 4: Analytics Processing" -ForegroundColor Yellow
$analytics = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Processed tick"
$analyticsCount = ($analytics | Measure-Object).Count
if ($analyticsCount -gt 0) {
    Write-Host "✓ Analytics processing ticks ($analyticsCount in last 100 lines)" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ Analytics not processing" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 5: Snapshots Saved (CRITICAL TEST)
Write-Host "Test 5: Snapshots Saved to Redis" -ForegroundColor Yellow
$snapshots = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Saved snapshot"
$snapshotCount = ($snapshots | Measure-Object).Count
if ($snapshotCount -gt 0) {
    Write-Host "✓ Snapshots being saved ($snapshotCount in last 100 lines)" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ Snapshots NOT being saved - THIS IS THE PROBLEM!" -ForegroundColor Red
    Write-Host "  → Analytics service needs to be rebuilt" -ForegroundColor Yellow
    $testFailed++
}
Write-Host ""

# Test 6: Redis Has Data
Write-Host "Test 6: Redis Has Snapshot Data" -ForegroundColor Yellow
$redisKeys = docker exec finbot-redis redis-cli KEYS "latest_snapshot:*" 2>&1
$keyCount = ($redisKeys | Measure-Object).Count
if ($keyCount -gt 1) {
    Write-Host "✓ Redis has snapshots:" -ForegroundColor Green
    $redisKeys | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
    $testPassed++
} else {
    Write-Host "✗ Redis has NO snapshots" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 7: BTC Snapshot
Write-Host "Test 7: BTC Snapshot Exists" -ForegroundColor Yellow
$btcExists = docker exec finbot-redis redis-cli EXISTS "latest_snapshot:BTC" 2>&1
if ($btcExists -match "1") {
    Write-Host "✓ BTC snapshot exists in Redis" -ForegroundColor Green
    Write-Host "Sample data:" -ForegroundColor Gray
    $btcData = docker exec finbot-redis redis-cli GET "latest_snapshot:BTC" 2>&1
    Write-Host ($btcData.Substring(0, [Math]::Min(150, $btcData.Length)) + "...") -ForegroundColor Gray
    $testPassed++
} else {
    Write-Host "✗ BTC snapshot missing" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 8: WebSocket API Running
Write-Host "Test 8: WebSocket API Running" -ForegroundColor Yellow
$wsApi = docker compose ps websocket-api 2>&1
if ($wsApi -match "Up") {
    Write-Host "✓ WebSocket API is running" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "✗ WebSocket API is down" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Test 9: Dashboard Accessible
Write-Host "Test 9: Dashboard Accessible" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 2 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Dashboard is accessible" -ForegroundColor Green
        $testPassed++
    }
} catch {
    Write-Host "✗ Dashboard is not accessible" -ForegroundColor Red
    $testFailed++
}
Write-Host ""

# Summary
Write-Host "================================" -ForegroundColor Cyan
Write-Host "📊 Test Results" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Passed: $testPassed" -ForegroundColor Green
Write-Host "Failed: $testFailed" -ForegroundColor Red
Write-Host ""

if ($testFailed -eq 0) {
    Write-Host "✅ All tests passed! System should be working." -ForegroundColor Green
    Write-Host ""
    Write-Host "If frontend still shows 'Waiting for data':" -ForegroundColor Yellow
    Write-Host "1. Hard refresh browser (Ctrl+Shift+R)"
    Write-Host "2. Check browser console for errors (F12)"
    Write-Host "3. Verify WebSocket connection in Network tab"
} else {
    Write-Host "❌ Some tests failed. Issues found:" -ForegroundColor Red
    Write-Host ""
    
    if ($snapshotCount -eq 0) {
        Write-Host "🔧 CRITICAL: Snapshots not being saved!" -ForegroundColor Yellow
        Write-Host "Fix: Run these commands:" -ForegroundColor White
        Write-Host "  docker compose build analytics-service" -ForegroundColor Cyan
        Write-Host "  docker compose up -d analytics-service" -ForegroundColor Cyan
        Write-Host "  docker compose logs -f analytics-service" -ForegroundColor Cyan
        Write-Host ""
    }
    
    if ($keyCount -le 1) {
        Write-Host "🔧 Redis has no data" -ForegroundColor Yellow
        Write-Host "This means snapshots aren't being saved." -ForegroundColor White
        Write-Host ""
    }
}

Write-Host ""
Write-Host "🔍 Quick Debug Commands:" -ForegroundColor Cyan
Write-Host "  docker compose logs -f analytics-service    # Watch analytics logs"
Write-Host "  docker compose logs -f websocket-api        # Watch WebSocket logs"
Write-Host "  docker exec -it finbot-redis redis-cli      # Connect to Redis"
Write-Host ""
