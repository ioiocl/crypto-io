# Finbot End-to-End Integration Test
Write-Host ""
Write-Host "Finbot End-to-End Integration Test" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

$testPassed = 0
$testFailed = 0

# Test 1: Binance Connection
Write-Host "Test 1: Binance Connection" -ForegroundColor Yellow
$binance = docker compose logs --tail=50 ingestion-service 2>&1 | Select-String "Connected to Binance"
if ($binance) {
    Write-Host "  PASS - Connected to Binance" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - Not connected" -ForegroundColor Red
    $testFailed++
}

# Test 2: Receiving Ticks
Write-Host "Test 2: Receiving Ticks" -ForegroundColor Yellow
$ticks = docker compose logs --tail=100 ingestion-service 2>&1 | Select-String "Processed Binance ticker"
if ($ticks.Count -gt 0) {
    Write-Host "  PASS - Received $($ticks.Count) tickers" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - No tickers received" -ForegroundColor Red
    $testFailed++
}

# Test 3: Analytics Processing
Write-Host "Test 3: Analytics Processing" -ForegroundColor Yellow
$analytics = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Processed tick"
if ($analytics.Count -gt 0) {
    Write-Host "  PASS - Analytics processing ($($analytics.Count) ticks)" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - Analytics not processing" -ForegroundColor Red
    $testFailed++
}

# Test 4: CRITICAL - Snapshots Saved
Write-Host "Test 4: Snapshots Saved (CRITICAL)" -ForegroundColor Yellow
$saved = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Saved snapshot"
if ($saved.Count -gt 0) {
    Write-Host "  PASS - Saved $($saved.Count) snapshots" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - NO SNAPSHOTS SAVED!" -ForegroundColor Red
    Write-Host "  This is why frontend shows no data!" -ForegroundColor Red
    $testFailed++
}

# Test 5: Redis Has Data
Write-Host "Test 5: Redis Has Data" -ForegroundColor Yellow
$keys = docker exec finbot-redis redis-cli KEYS "latest_snapshot:*" 2>&1 | Where-Object { $_ -match "latest_snapshot:" }
if ($keys.Count -gt 0) {
    Write-Host "  PASS - Found $($keys.Count) snapshots in Redis" -ForegroundColor Green
    $keys | ForEach-Object { Write-Host "    - $_" -ForegroundColor Gray }
    $testPassed++
} else {
    Write-Host "  FAIL - Redis has no snapshots" -ForegroundColor Red
    $testFailed++
}

# Test 6: BTC Snapshot Exists
Write-Host "Test 6: BTC Snapshot Exists" -ForegroundColor Yellow
$btc = docker exec finbot-redis redis-cli EXISTS "latest_snapshot:BTC" 2>&1
if ($btc -match "1") {
    Write-Host "  PASS - BTC snapshot exists" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - BTC snapshot missing" -ForegroundColor Red
    $testFailed++
}

# Test 7: WebSocket API Running
Write-Host "Test 7: WebSocket API Running" -ForegroundColor Yellow
$ws = docker compose ps websocket-api 2>&1
if ($ws -match "Up") {
    Write-Host "  PASS - WebSocket API is running" -ForegroundColor Green
    $testPassed++
} else {
    Write-Host "  FAIL - WebSocket API is down" -ForegroundColor Red
    $testFailed++
}

# Test 8: Dashboard Accessible
Write-Host "Test 8: Dashboard Accessible" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "  PASS - Dashboard accessible" -ForegroundColor Green
        $testPassed++
    }
} catch {
    Write-Host "  FAIL - Dashboard not accessible" -ForegroundColor Red
    $testFailed++
}

# Summary
Write-Host ""
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Passed: $testPassed" -ForegroundColor Green
Write-Host "Failed: $testFailed" -ForegroundColor Red
Write-Host ""

if ($testFailed -eq 0) {
    Write-Host "SUCCESS - All tests passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Open http://localhost:3000 to see data" -ForegroundColor Yellow
    Write-Host "If still showing 'Waiting', hard refresh (Ctrl+Shift+R)" -ForegroundColor Yellow
} else {
    Write-Host "FAILED - Issues detected" -ForegroundColor Red
    Write-Host ""
    
    if ($saved.Count -eq 0) {
        Write-Host "PRIMARY ISSUE: Snapshots NOT being saved!" -ForegroundColor Red
        Write-Host ""
        Write-Host "FIX:" -ForegroundColor Yellow
        Write-Host "  1. docker compose build analytics-service" -ForegroundColor Cyan
        Write-Host "  2. docker compose up -d analytics-service" -ForegroundColor Cyan
        Write-Host "  3. docker compose logs -f analytics-service" -ForegroundColor Cyan
        Write-Host "  4. Look for 'Saved snapshot for BTC' messages" -ForegroundColor White
        Write-Host "  5. Run this test again: .\test-simple.ps1" -ForegroundColor Cyan
    }
}

Write-Host ""
