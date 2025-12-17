# Forecast Configuration Guide

## Overview

The Finbot analytics system now supports configurable forecast horizons through environment variables. This allows you to easily adjust the time period for predictions without modifying code.

## Configuration Variables

### Monte Carlo Simulation

| Variable | Default | Description |
|----------|---------|-------------|
| `MONTE_CARLO_HORIZON_DAYS` | 7 | Number of days to simulate (1 week) |
| `MONTE_CARLO_SIMULATIONS` | 10000 | Number of simulation runs |

### ARIMA Forecast

| Variable | Default | Description |
|----------|---------|-------------|
| `ARIMA_HORIZON_PERIODS` | 7 | Number of periods to forecast |

## Common Configurations

### 1 Week (Default)
```bash
MONTE_CARLO_HORIZON_DAYS=7
ARIMA_HORIZON_PERIODS=7
```

**Use case:** Short-term trading, day trading strategies

**Expected results:**
- Lower VaR (less time = less risk)
- Lower expected returns
- More accurate predictions

### 2 Weeks
```bash
MONTE_CARLO_HORIZON_DAYS=14
ARIMA_HORIZON_PERIODS=14
```

**Use case:** Swing trading, medium-term positions

### 1 Month
```bash
MONTE_CARLO_HORIZON_DAYS=30
ARIMA_HORIZON_PERIODS=30
```

**Use case:** Position trading, long-term analysis

**Expected results:**
- Higher VaR (more time = more risk)
- Higher expected returns
- Less accurate predictions (more uncertainty)

### 3 Months
```bash
MONTE_CARLO_HORIZON_DAYS=90
ARIMA_HORIZON_PERIODS=90
```

**Use case:** Long-term investment analysis

## How to Change Configuration

### Method 1: Edit .env file

1. Open `.env` file in the root directory
2. Modify the values:
   ```bash
   MONTE_CARLO_HORIZON_DAYS=14
   ARIMA_HORIZON_PERIODS=14
   ```
3. Restart services:
   ```bash
   docker compose down
   docker compose up --build
   ```

### Method 2: Docker Compose Override

Create `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  analytics-service:
    environment:
      - MONTE_CARLO_HORIZON_DAYS=14
      - ARIMA_HORIZON_PERIODS=14
```

Then restart:
```bash
docker compose up -d
```

### Method 3: Command Line

```bash
MONTE_CARLO_HORIZON_DAYS=14 ARIMA_HORIZON_PERIODS=14 docker compose up
```

## Impact on Results

### VaR Scaling

VaR scales with the square root of time:

```
VaR(T days) = VaR(1 day) × √T
```

**Examples:**
- 7 days: VaR × √7 = VaR × 2.65
- 14 days: VaR × √14 = VaR × 3.74
- 30 days: VaR × √30 = VaR × 5.48

### Expected Return Scaling

Expected return scales linearly with time:

```
Return(T days) = Return(1 day) × T
```

**Examples:**
- 7 days: Return × 7
- 14 days: Return × 14
- 30 days: Return × 30

### Volatility Scaling

Volatility scales with the square root of time:

```
Vol(T days) = Vol(1 day) × √T
```

## Verification

After changing configuration, verify in logs:

```bash
docker compose logs analytics-service | grep "configuration"
```

You should see:
```
Using Monte Carlo configuration: simulations=10000, horizon=7 days
Using ARIMA configuration: horizon=7 periods
```

## Recommendations by Trading Style

| Trading Style | Horizon | Rationale |
|---------------|---------|-----------|
| **Scalping** | 1-3 days | Very short-term, high frequency |
| **Day Trading** | 5-7 days | Short-term trends |
| **Swing Trading** | 14-21 days | Medium-term patterns |
| **Position Trading** | 30-60 days | Long-term trends |
| **Buy & Hold** | 90+ days | Investment analysis |

## Troubleshooting

### Issue: Changes not taking effect

**Solution:** Rebuild containers
```bash
docker compose down
docker compose up --build
```

### Issue: High AIC values

**Solution:** Reduce horizon for more accurate predictions
```bash
ARIMA_HORIZON_PERIODS=5
```

### Issue: VaR too high/low

**Solution:** Adjust horizon to match your risk tolerance
- Lower horizon = Lower VaR
- Higher horizon = Higher VaR

## Advanced: Multiple Horizons

To analyze multiple time periods simultaneously, you would need to:

1. Run multiple instances of analytics service
2. Each with different horizon configuration
3. Store results with horizon tag in Redis

Example docker-compose setup:
```yaml
analytics-service-weekly:
  environment:
    - MONTE_CARLO_HORIZON_DAYS=7

analytics-service-monthly:
  environment:
    - MONTE_CARLO_HORIZON_DAYS=30
```

## Notes

- **Data frequency matters**: If your data is per-minute, 7 periods = 7 minutes, not 7 days
- **Crypto vs Stocks**: Crypto operates 24/7, stocks only during market hours
- **Confidence decreases** with longer horizons (more uncertainty)
- **Rebalance regularly**: Update forecasts as new data arrives

## Support

For questions or issues, check:
- Main README.md
- ARCHITECTURE.md
- Project logs: `docker compose logs -f analytics-service`
