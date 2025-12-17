# Update .env file to include crypto symbols

Write-Host "Updating .env file with crypto symbols..." -ForegroundColor Yellow

$envContent = Get-Content .env -Raw

# Update ANALYTICS_SYMBOLS
$envContent = $envContent -replace 'ANALYTICS_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN', 'ANALYTICS_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN,BTC,ETH,BNB,SOL,XRP'

# Update BROADCAST_SYMBOLS
$envContent = $envContent -replace 'BROADCAST_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN', 'BROADCAST_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN,BTC,ETH,BNB,SOL,XRP'

# Add BINANCE_SYMBOLS if not present
if ($envContent -notmatch 'BINANCE_SYMBOLS') {
    $envContent += "`nBINANCE_SYMBOLS=btc,eth,bnb,sol,xrp`n"
}

# Save updated content
$envContent | Set-Content .env -NoNewline

Write-Host "Updated .env file!" -ForegroundColor Green
Write-Host ""
Write-Host "Changes made:" -ForegroundColor Cyan
Write-Host "  - ANALYTICS_SYMBOLS now includes: BTC,ETH,BNB,SOL,XRP" -ForegroundColor White
Write-Host "  - BROADCAST_SYMBOLS now includes: BTC,ETH,BNB,SOL,XRP" -ForegroundColor White
Write-Host "  - BINANCE_SYMBOLS added: btc,eth,bnb,sol,xrp" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. docker compose down" -ForegroundColor Cyan
Write-Host "  2. docker compose up -d" -ForegroundColor Cyan
Write-Host "  3. .\test-simple.ps1" -ForegroundColor Cyan
Write-Host ""
