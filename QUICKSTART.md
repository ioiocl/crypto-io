# Finbot Quick Start Guide

Get Finbot running in 5 minutes!

## Prerequisites

- Docker Desktop installed and running
- 8GB RAM minimum
- Ports available: 3000, 6379, 8080, 8081, 8082

## Step 1: Clone and Configure

```bash
cd C:\Users\avasquezp\Documents\tmp\Finbot

# Copy environment file
copy .env.example .env
```

**Important**: The `.env` file already contains the Polygon API key. You can modify the symbols if needed:

```env
POLYGON_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
```

## Step 2: Start the Application

```bash
docker compose up --build
```

This will:
- Build all 4 services (takes 5-10 minutes first time)
- Start Redis
- Start ingestion, analytics, and WebSocket services
- Start the React dashboard

## Step 3: Access the Dashboard

Open your browser to:
```
http://localhost:3000
```

You should see:
- Real-time price updates
- Bayesian analysis metrics
- ARIMA forecasts
- Monte Carlo risk assessments
- Interactive charts

## What You'll See

### Dashboard Features

1. **Symbol Selector**: Switch between AAPL, GOOGL, MSFT, TSLA, AMZN
2. **Live Indicator**: Green dot shows real-time connection
3. **Market State**: BULLISH, BEARISH, or NEUTRAL classification
4. **Price Chart**: Real-time ARIMA forecast visualization
5. **Analytics Cards**:
   - Bayesian metrics (drift, volatility)
   - Monte Carlo results (VaR, probabilities)
   - ARIMA forecast details
6. **Risk Metrics**: VaR 95%, VaR 99%, CVaR

### Expected Behavior

- **First 30 seconds**: "Waiting for data..." (collecting initial ticks)
- **After 30 seconds**: First snapshot appears
- **Every 5 seconds**: Analytics update
- **Every 1 second**: Dashboard refreshes

## Verify Services

Check all services are running:

```bash
docker compose ps
```

You should see:
```
finbot-redis              running
finbot-ingestion          running
finbot-analytics          running
finbot-websocket-api      running
finbot-dashboard          running
```

## View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f ingestion-service
docker compose logs -f analytics-service
docker compose logs -f websocket-api
```

## Test WebSocket Connection

Open browser console (F12) and run:

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/market/AAPL');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

You should see JSON snapshots every second.

## Troubleshooting

### Issue: "Waiting for data..." never ends

**Solution**:
1. Check ingestion service logs:
   ```bash
   docker compose logs ingestion-service
   ```
2. Verify Polygon API key is valid
3. Check internet connection

### Issue: Dashboard shows connection error

**Solution**:
1. Verify WebSocket API is running:
   ```bash
   docker compose ps websocket-api
   ```
2. Check port 8080 is not blocked by firewall
3. Restart services:
   ```bash
   docker compose restart websocket-api
   ```

### Issue: Services won't start

**Solution**:
1. Check Docker is running
2. Verify ports are available:
   ```bash
   netstat -an | findstr "3000 6379 8080"
   ```
3. Increase Docker memory to 4GB minimum
4. Clean and rebuild:
   ```bash
   docker compose down -v
   docker compose up --build
   ```

### Issue: Redis connection failed

**Solution**:
1. Check Redis is healthy:
   ```bash
   docker compose ps redis
   ```
2. Restart Redis:
   ```bash
   docker compose restart redis
   ```

## Stop the Application

```bash
# Stop all services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v
```

## Next Steps

### Customize Symbols

Edit `.env` file:
```env
POLYGON_SYMBOLS=NVDA,AMD,INTC
ANALYTICS_SYMBOLS=NVDA,AMD,INTC
BROADCAST_SYMBOLS=NVDA,AMD,INTC
```

Restart:
```bash
docker compose down
docker compose up -d
```

### Adjust Update Frequency

Edit `.env`:
```env
SNAPSHOT_INTERVAL=10s    # Analytics update every 10 seconds
BROADCAST_INTERVAL=2s    # Dashboard refresh every 2 seconds
```

### View Redis Data

```bash
docker compose exec redis redis-cli

# List all keys
KEYS *

# Get snapshot for AAPL
GET latest_snapshot:AAPL

# Monitor pub/sub
SUBSCRIBE market-stream
```

## Development Mode

### Run Services Individually

**Terminal 1 - Redis**:
```bash
docker run -p 6379:6379 redis:7-alpine
```

**Terminal 2 - Ingestion**:
```bash
cd ingestion-service
mvn quarkus:dev
```

**Terminal 3 - Analytics**:
```bash
cd analytics-service
mvn quarkus:dev
```

**Terminal 4 - WebSocket API**:
```bash
cd websocket-api
mvn quarkus:dev
```

**Terminal 5 - Dashboard**:
```bash
cd dashboard
npm install
npm run dev
```

## Performance Tips

1. **Reduce symbols**: Start with 1-2 symbols for testing
2. **Increase intervals**: Use longer update intervals
3. **Allocate more memory**: Give Docker 6-8GB RAM
4. **Use SSD**: Better I/O performance
5. **Close other apps**: Free up system resources

## Health Checks

### Service Health Endpoints

- Ingestion: http://localhost:8081/q/health
- Analytics: http://localhost:8082/q/health
- WebSocket API: http://localhost:8080/q/health

### Metrics Endpoints

- Ingestion: http://localhost:8081/q/metrics
- Analytics: http://localhost:8082/q/metrics
- WebSocket API: http://localhost:8080/q/metrics

## Common Commands

```bash
# View resource usage
docker stats

# Clean up everything
docker compose down -v
docker system prune -a

# Rebuild specific service
docker compose up -d --build ingestion-service

# Scale analytics service
docker compose up -d --scale analytics-service=2

# Export logs
docker compose logs > finbot-logs.txt
```

## Getting Help

1. Check logs: `docker compose logs -f`
2. Review README.md for detailed documentation
3. Check ARCHITECTURE.md for system design
4. Verify environment variables in `.env`
5. Ensure Docker has sufficient resources

## Success Indicators

✅ All 5 containers running
✅ Dashboard loads at http://localhost:3000
✅ Live indicator shows green
✅ Data appears within 30 seconds
✅ Charts update automatically
✅ No errors in logs

## What's Next?

- Read [README.md](README.md) for full documentation
- Explore [ARCHITECTURE.md](ARCHITECTURE.md) for system design
- Deploy to cloud with Terraform (see `terraform/` folder)
- Customize analytics parameters
- Add more symbols
- Integrate with your own systems

Enjoy using Finbot! 🚀📈
