# ABC Analysis Quick Reference

## What is ABC?

**ABC = ARIMA + Bayes + Carlo**

A three-stage integrated pipeline for financial market analysis:
1. **ARIMA**: Detects price trends and structural breaks
2. **Bayesian**: Calculates momentum with ARIMA-informed priors
3. **Monte Carlo**: Simulates price paths using Bayesian momentum

## Key Files

| File | Purpose |
|------|---------|
| `ABCAnalyzer.java` | Main orchestrator - integrates all three stages |
| `ARIMASignal.java` | Stage 1 output - trend and structural breaks |
| `MomentumMetrics.java` | Stage 2 output - drift and volatility |
| `MarketPrediction.java` | Stage 3 output - probabilities and targets |
| `ABCAnalysisResult.java` | Complete ABC result with all stages |
| `MarketSnapshot.java` | Updated to include ABC analysis |
| `MarketAnalysisService.java` | Updated to use ABC analyzer |

## Market Regimes

| Regime | When It Happens |
|--------|-----------------|
| `BULLISH_STABLE` | Strong uptrend, low volatility |
| `BULLISH_VOLATILE` | Uptrend with high uncertainty |
| `BEARISH_STABLE` | Strong downtrend, low volatility |
| `BEARISH_VOLATILE` | Downtrend with high uncertainty |
| `NEUTRAL_STABLE` | Sideways market, low volatility |
| `NEUTRAL_VOLATILE` | Sideways market, high uncertainty |
| `REGIME_CHANGE` | Structural break detected (crash, news) |
| `HIGH_VOLATILITY` | Extreme uncertainty (>50% vol) |

## Structural Break Detection

**CUSUM Algorithm** monitors last 30% of price data:
- Threshold: **3 standard deviations**
- Triggers: Market crashes, earnings, Fed announcements
- Action: Sets `needsRecalibration = true`

## Integration Confidence

```
confidence = √(ARIMA_confidence × Bayesian_confidence) × stability_factor
```

- **High confidence (>0.8)**: Trust the predictions
- **Medium confidence (0.5-0.8)**: Use with caution
- **Low confidence (<0.5)**: High uncertainty, recalibration needed

## API Response Structure

```json
{
  "abcAnalysis": {
    "arimaSignal": {
      "trendPercentage": 12.5,
      "structuralBreakDetected": false,
      "description": "Price increasing 12.5% in trend"
    },
    "momentumMetrics": {
      "drift": 0.045,
      "volatility": 0.25
    },
    "marketPrediction": {
      "probabilityUp": 0.68,
      "expectedPriceChangePercent": 1.28,
      "priceTargets": [...]
    },
    "marketRegime": "BULLISH_STABLE",
    "needsRecalibration": false
  }
}
```

## How to Use

### 1. Check Market Regime
```java
String regime = snapshot.getAbcAnalysis().getMarketRegime();
if (regime.equals("BULLISH_STABLE")) {
    // Consider long positions
}
```

### 2. Check Recalibration Flag
```java
if (snapshot.getAbcAnalysis().getNeedsRecalibration()) {
    log.warn("Structural break detected - exercise caution");
}
```

### 3. Get Price Targets
```java
List<PriceTarget> targets = snapshot.getAbcAnalysis()
    .getMarketPrediction()
    .getPriceTargets();
    
// 95th percentile = optimistic target
// 5th percentile = pessimistic target
```

### 4. Check Confidence
```java
BigDecimal confidence = snapshot.getAbcAnalysis()
    .getAbcIntegrationConfidence();
    
if (confidence.compareTo(BigDecimal.valueOf(0.8)) > 0) {
    // High confidence - trust the prediction
}
```

## Monitoring

### Log Messages

✅ **Normal Operation**
```
✓ ABC Analysis - Regime: BULLISH_STABLE - Confidence: 0.85 - ARIMA: Price increasing 12.5% in trend
```

⚠️ **Recalibration Needed**
```
⚠️ RECALIBRATION NEEDED! ABC Analysis - Regime: REGIME_CHANGE - Confidence: 0.60 - ARIMA: Price decreasing 15.2% in trend [STRUCTURAL BREAK DETECTED]
```

## Configuration

```properties
# Monte Carlo
monte.carlo.simulations=10000
monte.carlo.horizon.days=7

# ARIMA
arima.horizon.periods=7

# Analytics
analytics.snapshot.interval=5s
analytics.symbols=AAPL,GOOGL,MSFT,TSLA,AMZN
```

## Trading Signals

### Strong Buy
- Regime: `BULLISH_STABLE`
- Confidence: >0.8
- ProbabilityUp: >0.7
- No structural break

### Buy
- Regime: `BULLISH_VOLATILE`
- Confidence: >0.6
- ProbabilityUp: >0.6

### Hold
- Regime: `NEUTRAL_STABLE` or `NEUTRAL_VOLATILE`
- Mixed signals

### Sell
- Regime: `BEARISH_VOLATILE`
- Confidence: >0.6
- ProbabilityDown: >0.6

### Strong Sell
- Regime: `BEARISH_STABLE`
- Confidence: >0.8
- ProbabilityDown: >0.7
- No structural break

### Caution (Exit All)
- Regime: `REGIME_CHANGE` or `HIGH_VOLATILITY`
- NeedsRecalibration: true
- Structural break detected

## Key Metrics Explained

### Drift (Momentum)
- **Positive**: Upward momentum
- **Negative**: Downward momentum
- **Annualized**: Multiply by 252 trading days

### Volatility
- **Low (<0.20)**: Stable market
- **Medium (0.20-0.40)**: Normal market
- **High (>0.40)**: Turbulent market

### Trend Percentage
- **>5%**: Strong uptrend
- **-5% to 5%**: Sideways
- **<-5%**: Strong downtrend

### CUSUM Statistic
- **<threshold**: Market stable
- **>threshold**: Structural break detected

## Adaptations from Sports to Finance

| Sports Concept | Finance Equivalent |
|----------------|-------------------|
| Goal-scoring patterns | Price movements |
| Possession changes | Returns/momentum |
| Match momentum | Market momentum |
| Red card/injury | Market crash/news |
| Final score prediction | Price target |
| Win/Draw/Loss | Up/Neutral/Down |
| Structural break (tactical change) | Regime change (Fed, earnings) |

## Common Patterns

### Bull Market
- Trend: +5% to +15%
- Drift: +0.10 to +0.30
- Volatility: 0.15 to 0.25
- ProbUp: 0.65 to 0.80

### Bear Market
- Trend: -15% to -5%
- Drift: -0.30 to -0.10
- Volatility: 0.25 to 0.40
- ProbDown: 0.65 to 0.80

### Market Crash
- Structural break: true
- CUSUM: >3σ
- Volatility: >0.50
- Regime: REGIME_CHANGE

### Sideways Market
- Trend: -2% to +2%
- Drift: -0.05 to +0.05
- Volatility: 0.10 to 0.20
- ProbUp ≈ ProbDown

## Troubleshooting

### Low Confidence (<0.5)
- **Cause**: Insufficient data or high uncertainty
- **Action**: Wait for more data or reduce position size

### Frequent Recalibrations
- **Cause**: High market volatility or news-driven market
- **Action**: Reduce trading frequency, use wider stops

### Contradictory Signals
- **Cause**: Regime transition or mixed market conditions
- **Action**: Stay neutral, wait for clarity

### High Volatility (>0.50)
- **Cause**: Market stress, earnings season, macro events
- **Action**: Reduce leverage, increase cash position

## Best Practices

1. **Always check recalibration flag** before making decisions
2. **Use confidence scores** to size positions
3. **Monitor regime changes** for early warnings
4. **Combine with other indicators** (don't rely solely on ABC)
5. **Backtest** ABC signals on historical data
6. **Set alerts** for structural breaks
7. **Review logs** regularly for patterns
8. **Adjust thresholds** based on asset class (stocks vs crypto)

## Performance Metrics

Track these metrics to evaluate ABC performance:

- **Prediction accuracy**: % of correct direction predictions
- **Confidence calibration**: Are 80% confidence predictions right 80% of time?
- **Structural break detection**: False positive/negative rate
- **Regime classification**: How long does each regime last?
- **Price target accuracy**: How close are percentile predictions?

## Next Steps

1. Review `ABC_ANALYSIS.md` for detailed documentation
2. Check `ABCAnalyzer.java` for implementation details
3. Monitor logs for ABC analysis results
4. Backtest ABC signals on historical data
5. Integrate ABC signals into trading strategy
