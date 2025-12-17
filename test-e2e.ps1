# Finbot End-to-End Integration Test
# Tests the complete data flow: Binance → Ingestion → Analytics → Redis → WebSocket → Frontend

Write-Host ""
Write-Host "Finbot End-to-End Integration Test" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "Testing: Binance to Ingestion to Analytics to Redis to WebSocket to Frontend" -ForegroundColor Gray
Write-Host ""

$testPassed = 0
$testFailed = 0
$criticalFailed = $false

# Helper function for test results
function Test-Step {
    param(
        [string]$Name,
        [scriptblock]$Test,
        [bool]$Critical = $false
    )
    
    Write-Host "Testing: $Name" -ForegroundColor Yellow
    $result = & $Test
    
    if ($result) {
        Write-Host "  ✓ PASS" -ForegroundColor Green
        $script:testPassed++
        return $true
    } else {
        if ($Critical) {
            Write-Host "  ✗ FAIL (CRITICAL)" -ForegroundColor Red
            $script:criticalFailed = $true
        } else {
            Write-Host "  ✗ FAIL" -ForegroundColor Red
        }
        $script:testFailed++
        return $false
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PHASE 1: Backend Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Services Running
Test-Step "All services are running" {
    $services = docker compose ps 2>&1
    return ($services -match "ingestion.*Up" -and $services -match "analytics.*Up" -and $services -match "websocket-api.*Up")
} -Critical $true

# Test 2: Binance Connection
Test-Step "Connected to Binance WebSocket" {
    $logs = docker compose logs --tail=50 ingestion-service 2>&1 | Select-String "Connected to Binance"
    return $logs.Count -gt 0
} -Critical $true

# Test 3: Receiving Ticks
Test-Step "Receiving crypto tickers from Binance" {
    $ticks = docker compose logs --tail=100 ingestion-service 2>&1 | Select-String "Processed Binance ticker"
    if ($ticks.Count -gt 0) {
        Write-Host "    Found $($ticks.Count) tickers in last 100 log lines" -ForegroundColor Gray
        return $true
    }
    return $false
} -Critical $true

# Test 4: Publishing to Redis Stream
Test-Step "Publishing ticks to Redis stream" {
    $published = docker compose logs --tail=100 ingestion-service 2>&1 | Select-String "Published tick"
    return $published.Count -gt 0
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "PHASE 2: Analytics Processing" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Test 5: Analytics Processing Ticks
Test-Step "Analytics receiving and processing ticks" {
    $processed = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Processed tick"
    if ($processed.Count -gt 0) {
        Write-Host "    Processed $($processed.Count) ticks" -ForegroundColor Gray
        return $true
    }
    return $false
} -Critical $true

# Test 6: Generating Snapshots
Test-Step "Generating market snapshots" {
    $generating = docker compose logs --tail=50 analytics-service 2>&1 | Select-String "Generating snapshots"
    return $generating.Count -gt 0
}

# Test 7: CRITICAL - Saving Snapshots
Test-Step "Saving snapshots to Redis (CRITICAL)" {
    $saved = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Saved snapshot"
    if ($saved.Count -gt 0) {
        Write-Host "    Saved $($saved.Count) snapshots" -ForegroundColor Gray
        return $true
    } else {
        Write-Host "    ⚠️  NO SNAPSHOTS SAVED - Analytics needs rebuild!" -ForegroundColor Red
        return $false
    }
} -Critical $true

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "PHASE 3: Redis Data Storage" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Test 8: Redis Has Snapshots
Test-Step "Redis contains snapshot data" {
    $keys = docker exec finbot-redis redis-cli KEYS "latest_snapshot:*" 2>&1 | Where-Object { $_ -match "latest_snapshot:" }
    if ($keys.Count -gt 0) {
        Write-Host "    Found snapshots for:" -ForegroundColor Gray
        $keys | ForEach-Object { Write-Host "      - $_" -ForegroundColor Gray }
        return $true
    }
    return $false
} -Critical $true

# Test 9: BTC Snapshot Exists
$btcExists = Test-Step "BTC snapshot exists in Redis" {
    $exists = docker exec finbot-redis redis-cli EXISTS "latest_snapshot:BTC" 2>&1
    return $exists -match "1"
} -Critical $true

# Test 10: BTC Snapshot Has Valid Data
if ($btcExists) {
    Test-Step "BTC snapshot contains valid JSON data" {
        $data = docker exec finbot-redis redis-cli GET "latest_snapshot:BTC" 2>&1
        if ($data -match "symbol.*BTC" -and $data -match "currentPrice") {
            Write-Host "    Sample: $($data.Substring(0, [Math]::Min(100, $data.Length)))..." -ForegroundColor Gray
            return $true
        }
        return $false
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "PHASE 4: WebSocket API" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Test 11: WebSocket API Running
Test-Step "WebSocket API service is running" {
    $ws = docker compose ps websocket-api 2>&1
    return $ws -match "Up"
} -Critical $true

# Test 12: WebSocket Broadcasting
Test-Step "WebSocket API broadcasting snapshots" {
    $broadcast = docker compose logs --tail=50 websocket-api 2>&1 | Select-String "broadcast"
    return $broadcast.Count -gt 0
}

# Test 13: WebSocket Health Check
Test-Step "WebSocket API health endpoint responding" {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/q/health" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PHASE 5: Frontend and WebSocket Connection" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 14: Dashboard Accessible
Test-Step "Dashboard is accessible at http://localhost:3000" {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
} -Critical $true

# Test 15: WebSocket Endpoint Available
Test-Step "WebSocket endpoint /ws/market/{symbol} available" {
    try {
        # Check if WebSocket port is listening
        $connection = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue
        return $connection.TcpTestSucceeded
    } catch {
        return $false
    }
}

# Test 16: Check for WebSocket Connections
Test-Step "Frontend clients connecting to WebSocket" {
    $connections = docker compose logs --tail=100 websocket-api 2>&1 | Select-String "Client connected"
    if ($connections.Count -gt 0) {
        Write-Host "    Found $($connections.Count) client connections" -ForegroundColor Gray
        return $true
    }
    return $false
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📊 TEST SUMMARY" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total Tests: $($testPassed + $testFailed)" -ForegroundColor White
Write-Host "Passed:      $testPassed" -ForegroundColor Green
Write-Host "Failed:      $testFailed" -ForegroundColor Red
Write-Host ""

if ($testFailed -eq 0) {
    Write-Host "✅ ALL TESTS PASSED!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🎉 System is working correctly!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Open browser: http://localhost:3000" -ForegroundColor White
    Write-Host "2. You should see crypto data (BTC, ETH, BNB, SOL, XRP)" -ForegroundColor White
    Write-Host "3. Data updates every 1 second" -ForegroundColor White
    Write-Host ""
    Write-Host "If frontend still shows 'Waiting for data':" -ForegroundColor Yellow
    Write-Host "  • Hard refresh: Ctrl+Shift+R" -ForegroundColor White
    Write-Host "  • Open DevTools (F12) → Console tab" -ForegroundColor White
    Write-Host "  • Check Network tab → WS (WebSocket)" -ForegroundColor White
    Write-Host "  • Look for connection to ws://localhost:8080/ws/market/BTC" -ForegroundColor White
} else {
    Write-Host "❌ TESTS FAILED" -ForegroundColor Red
    Write-Host ""
    
    if ($criticalFailed) {
        Write-Host "🚨 CRITICAL FAILURES DETECTED" -ForegroundColor Red
        Write-Host ""
        
        # Check specific failure
        $savedSnapshots = docker compose logs --tail=100 analytics-service 2>&1 | Select-String "Saved snapshot"
        if ($savedSnapshots.Count -eq 0) {
            Write-Host "🔧 PRIMARY ISSUE: Snapshots are NOT being saved to Redis" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "This is the root cause of frontend not showing data!" -ForegroundColor Red
            Write-Host ""
            Write-Host "FIX:" -ForegroundColor Green
            Write-Host "  1. Rebuild analytics service:" -ForegroundColor White
            Write-Host "     docker compose build analytics-service" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "  2. Restart the service:" -ForegroundColor White
            Write-Host "     docker compose up -d analytics-service" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "  3. Watch logs to verify fix:" -ForegroundColor White
            Write-Host "     docker compose logs -f analytics-service" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "  4. Look for 'Saved snapshot for BTC' messages" -ForegroundColor White
            Write-Host ""
            Write-Host "  5. Re-run this test:" -ForegroundColor White
            Write-Host "     .\test-e2e.ps1" -ForegroundColor Cyan
        }
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🔍 Debug Commands" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Watch analytics logs:" -ForegroundColor Yellow
Write-Host "  docker compose logs -f analytics-service" -ForegroundColor Cyan
Write-Host ""
Write-Host "Watch WebSocket logs:" -ForegroundColor Yellow
Write-Host "  docker compose logs -f websocket-api" -ForegroundColor Cyan
Write-Host ""
Write-Host "Check Redis data:" -ForegroundColor Yellow
Write-Host "  docker exec -it finbot-redis redis-cli" -ForegroundColor Cyan
Write-Host "  Then run: KEYS latest_snapshot:*" -ForegroundColor Gray
Write-Host "  Then run: GET latest_snapshot:BTC" -ForegroundColor Gray
Write-Host ""
Write-Host "Check all services status:" -ForegroundColor Yellow
Write-Host "  docker compose ps" -ForegroundColor Cyan
Write-Host ""
