# ABC Analysis: ARIMA-Bayes-Carlo Integration for Finance

## Overview

The ABC (ARIMA-Bayes-Carlo) analysis is an integrated three-stage analytical pipeline that provides real-time market predictions with adaptive learning capabilities. This system is adapted from sports analytics to financial markets, providing comprehensive risk assessment and trend detection.

## Architecture

```
Market Ticks (price, volume, bid/ask)
    ↓
┌─────────────────────────────────────────────────────────┐
│ Stage 1: ARIMA Analysis                                 │
│ - Detects trends in price patterns                     │
│ - Identifies structural breaks (CUSUM algorithm)       │
│ - Exports trend signals                                 │
└─────────────────────────────────────────────────────────┘
    ↓ (trend signal feeds Bayesian prior)
┌─────────────────────────────────────────────────────────┐
│ Stage 2: Bayesian Momentum Analysis                     │
│ - Updates momentum with ARIMA-informed prior           │
│ - Calculates drift (momentum) and volatility           │
│ - Adjusts confidence based on structural breaks        │
└─────────────────────────────────────────────────────────┘
    ↓ (dynamic probabilities)
┌─────────────────────────────────────────────────────────┐
│ Stage 3: Monte Carlo Simulation                         │
│ - Simulates 10,000 price paths                         │
│ - Uses Bayesian momentum to adjust probabilities       │
│ - Generates probability distributions                   │
└─────────────────────────────────────────────────────────┘
    ↓ (feedback loop)
┌─────────────────────────────────────────────────────────┐
│ Recalibration Trigger                                   │
│ - Structural break detected → reset models             │
│ - High volatility → reduce confidence                   │
└─────────────────────────────────────────────────────────┘
```

## Stage 1: ARIMA Analysis

### Purpose
Detect trends and structural breaks in market data to inform Bayesian priors.

### Implementation
- **Algorithm**: Holt's exponential smoothing (simplified ARIMA)
- **Structural Break Detection**: CUSUM (Cumulative Sum Control Chart)
- **Output**: ARIMASignal with trend percentage and break detection

### Example Output
```
"Price increasing 12.5% in trend"
"Price stable"
"Price decreasing 8.2% in trend [STRUCTURAL BREAK DETECTED]"
```

### Key Features
- Monitors last 30% of observations for sudden changes
- Threshold: 3 standard deviations
- Detects events like: market crashes, news events, regime changes

### Financial Adaptations
Unlike sports where goals are discrete events, financial markets have:
- **Continuous price movements**: ARIMA tracks price trends rather than event counts
- **Bid-ask spreads**: Incorporates liquidity signals
- **Volume patterns**: Detects unusual trading activity
- **Market microstructure**: Identifies structural breaks from news, earnings, or macro events

### Code Location
`analytics-service/src/main/java/cl/ioio/finbot/analytics/domain/ABCAnalyzer.java`

## Stage 2: Bayesian Momentum Analysis

### Purpose
Update market momentum using ARIMA trend as an informed prior.

### How ARIMA Feeds Bayes

```java
// ARIMA says: "Price increasing 12% in trend"
double arimaTrend = arimaSignal.getTrend().doubleValue();

// Bayesian prior mean is set from ARIMA trend
double priorMean = arimaTrend * 10.0;

// Prior variance adjusted by ARIMA confidence
double priorVariance = 0.01 * (2.0 - arimaConfidence);

// Bayesian update combines ARIMA prior with observed returns
double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
```

### Benefits
- **Adaptive**: Prior adjusts based on what's actually happening
- **Confident**: High ARIMA confidence → tighter prior
- **Cautious**: Structural breaks → reduced confidence

### Financial Adaptations
- **Log returns**: Uses log returns instead of raw price changes for better statistical properties
- **Annualized metrics**: Drift and volatility are annualized (252 trading days)
- **Risk-adjusted**: Accounts for both expected return (drift) and uncertainty (volatility)

### Code Location
`analytics-service/src/main/java/cl/ioio/finbot/analytics/domain/ABCAnalyzer.java`

## Stage 3: Monte Carlo Simulation

### Purpose
Simulate thousands of price paths using dynamic Bayesian probabilities.

### How Bayes Feeds Monte Carlo

```java
// Bayesian momentum (drift) adjusts price dynamics
double drift = momentum.getDrift().doubleValue();
double volatility = momentum.getVolatility().doubleValue();

// Monte Carlo simulates 10,000 price paths with Geometric Brownian Motion
// S(t) = S(0) * exp((μ - 0.5σ²)t + σW(t))
```

### Output
- Win/Loss/Neutral probabilities
- Expected price changes
- Price targets at various percentiles (5%, 25%, 50%, 75%, 95%)
- Value at Risk (VaR) metrics

### Financial Adaptations
- **Geometric Brownian Motion**: Standard model for stock prices (ensures prices stay positive)
- **Risk metrics**: VaR and CVaR for risk management
- **Price targets**: Actionable price levels for trading decisions
- **Probability distributions**: Full distribution of possible outcomes

### Code Location
`analytics-service/src/main/java/cl/ioio/finbot/analytics/domain/MonteCarloSimulator.java`

## Feedback Loop: Recalibration

### Trigger Conditions
1. **ARIMA detects structural break** → Models need recalibration
2. **High volatility (> 50%)** → Market is unpredictable

### Actions
- Reduce confidence scores
- Flag snapshot with `needsRecalibration: true`
- Log warning for monitoring

### Example Scenarios
- **Market crash**: Sudden drop in prices → structural break detected
- **Earnings announcement**: Change in price patterns → recalibration triggered
- **Fed announcement**: Regime change → ARIMA detects trend change
- **Flash crash**: High volatility → confidence reduced

## Integration Confidence

The ABC system calculates an overall confidence score:

```java
double arimaConf = arimaSignal.getConfidence();
double bayesConf = momentum.getConfidence();

// Penalize during structural breaks
double stabilityFactor = structuralBreak ? 0.7 : 1.0;

// Geometric mean with stability adjustment
double integrationConfidence = sqrt(arimaConf * bayesConf) * stabilityFactor;
```

## Market Regimes

The ABC system identifies the following market regimes:

| Regime | Description | Characteristics |
|--------|-------------|-----------------|
| **BULLISH_STABLE** | Strong uptrend with low volatility | Trend > 2%, Drift > 5%, ProbUp > 60%, Vol < 30% |
| **BULLISH_VOLATILE** | Uptrend with high volatility | Trend > 2%, Drift > 5%, ProbUp > 60%, Vol > 30% |
| **BEARISH_STABLE** | Strong downtrend with low volatility | Trend < -2%, Drift < -5%, ProbUp < 40%, Vol < 30% |
| **BEARISH_VOLATILE** | Downtrend with high volatility | Trend < -2%, Drift < -5%, ProbUp < 40%, Vol > 30% |
| **NEUTRAL_STABLE** | Sideways market with low volatility | Mixed signals, Vol < 30% |
| **NEUTRAL_VOLATILE** | Sideways market with high volatility | Mixed signals, Vol > 30% |
| **REGIME_CHANGE** | Structural break detected | CUSUM threshold exceeded |
| **HIGH_VOLATILITY** | Extreme uncertainty | Vol > 50% |

## API Response

The `MarketSnapshot` now includes ABC analysis:

```json
{
  "symbol": "AAPL",
  "timestamp": "2024-12-10T23:27:00Z",
  "currentPrice": 195.50,
  "abcAnalysis": {
    "arimaSignal": {
      "trend": 0.0025,
      "trendPercentage": 12.5,
      "structuralBreakDetected": false,
      "confidence": 0.85,
      "description": "Price increasing 12.5% in trend",
      "cusumStatistic": 2.1,
      "threshold": 3.0
    },
    "momentumMetrics": {
      "drift": 0.045,
      "volatility": 0.25,
      "confidence": 0.82,
      "priorMean": 0.025,
      "posteriorMean": 0.045,
      "priorVariance": 0.012,
      "posteriorVariance": 0.008
    },
    "marketPrediction": {
      "probabilityUp": 0.68,
      "probabilityDown": 0.28,
      "probabilityNeutral": 0.04,
      "expectedPriceChange": 2.50,
      "expectedPriceChangePercent": 1.28,
      "mostLikelyScenario": "UPWARD_MOVEMENT",
      "priceTargets": [
        {"percentile": 5, "price": 185.20, "changePercent": -5.27},
        {"percentile": 25, "price": 191.30, "changePercent": -2.15},
        {"percentile": 50, "price": 196.80, "changePercent": 0.67},
        {"percentile": 75, "price": 202.50, "changePercent": 3.58},
        {"percentile": 95, "price": 210.40, "changePercent": 7.62}
      ]
    },
    "abcIntegrationConfidence": 0.83,
    "needsRecalibration": false,
    "marketRegime": "BULLISH_STABLE"
  }
}
```

## Benefits of ABC Integration

### 1. Live Adaptation
- Models adjust in real-time to market dynamics
- Not just "price is up" but "how the market is evolving"

### 2. Structural Break Detection
- Identifies sudden changes (crashes, news events, regime shifts)
- Triggers recalibration automatically

### 3. Informed Priors
- Bayesian analysis uses ARIMA trends, not fixed assumptions
- More accurate momentum estimation

### 4. Complete Probability Distributions
- Not just point predictions
- Full distribution of possible outcomes
- Confidence intervals and percentiles

### 5. Feedback Loop
- Monte Carlo results can inform future ARIMA calibration
- Continuous improvement through adaptive learning

### 6. Risk Management
- VaR and CVaR metrics for portfolio risk
- Scenario analysis for stress testing
- Confidence-adjusted predictions

## Comparison: Before vs After ABC

| Aspect | Before | After ABC |
|--------|--------|-----------|
| **ARIMA** | Independent price forecast | Feeds Bayesian prior |
| **Bayesian** | Fixed prior (mean=0) | ARIMA-informed prior |
| **Monte Carlo** | Uses Bayesian momentum | Uses ARIMA-adjusted momentum |
| **Structural Breaks** | Not detected | CUSUM detection + recalibration |
| **Integration** | Separate components | Orchestrated pipeline |
| **Confidence** | Per-component only | Integrated confidence score |
| **Market Regime** | Simple classification | 8 distinct regimes |

## Usage

The ABC analyzer is automatically used in the `MarketAnalysisService`:

```java
ABCAnalysisResult abcResult = abcAnalyzer.analyze(
    prices,
    currentPrice
);

// Check if recalibration needed
if (abcResult.isNeedsRecalibration()) {
    log.warn("Structural break detected! ARIMA: {}", 
        abcResult.getArimaSignal().getDescription());
}
```

## Monitoring

Watch for these log messages:

- ✅ `✓ ABC Analysis - Regime: BULLISH_STABLE - Confidence: 0.85 - ARIMA: Price increasing 12.5% in trend`
- ⚠️ `⚠️ RECALIBRATION NEEDED! ABC Analysis - Regime: REGIME_CHANGE - Confidence: 0.60 - ARIMA: Price decreasing 15.2% in trend [STRUCTURAL BREAK DETECTED]`

## Configuration

Configure ABC analysis in `application.properties`:

```properties
# Monte Carlo configuration
monte.carlo.simulations=10000
monte.carlo.horizon.days=7

# ARIMA configuration
arima.horizon.periods=7

# Analytics configuration
analytics.snapshot.interval=5s
analytics.symbols=AAPL,GOOGL,MSFT,TSLA,AMZN
```

## Future Enhancements

1. **Adaptive Learning**: Use prediction errors to tune ARIMA parameters
2. **Multi-feature ARIMA**: Include volume, bid-ask spread, not just prices
3. **Bayesian Change Point Detection**: More sophisticated break detection
4. **Dynamic Recalibration**: Automatically reset models on breaks
5. **Historical Validation**: Backtest ABC predictions vs actual outcomes
6. **Machine Learning Integration**: Use ML to optimize CUSUM thresholds
7. **Multi-asset Correlation**: Detect regime changes across correlated assets
8. **Real-time Alerts**: Push notifications for structural breaks
9. **Portfolio-level ABC**: Aggregate ABC signals across portfolio
10. **Sentiment Integration**: Incorporate news sentiment into ARIMA signals

## Technical Details

### CUSUM Algorithm

The CUSUM (Cumulative Sum Control Chart) algorithm detects structural breaks:

```java
// Monitor last 30% of observations
int monitorStart = (int)(prices.length * 0.7);
double cusum = 0.0;

for (int i = monitorStart; i < prices.length; i++) {
    double deviation = (prices[i] - mean) / stdDev;
    cusum += deviation;
    maxCusum = Math.max(Math.abs(maxCusum), Math.abs(cusum));
}

// Threshold: 3 standard deviations
boolean structuralBreak = Math.abs(cusumStatistic) > (3.0 * stdDev);
```

### Bayesian Update

The Bayesian update combines prior (from ARIMA) with observed data:

```java
// Prior parameters (informed by ARIMA)
double priorMean = arimaTrend * 10.0;
double priorVariance = 0.01 * (2.0 - arimaConfidence);
double priorN = 1.0 + arimaConfidence;

// Posterior parameters (after observing data)
double posteriorN = priorN + sampleSize;
double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
double posteriorVariance = ((priorN * priorVariance + 
        sampleSize * sampleVariance + 
        (priorN * sampleSize / posteriorN) * 
        Math.pow(sampleMean - priorMean, 2)) / posteriorN);
```

### Geometric Brownian Motion

Monte Carlo simulation uses GBM for realistic price paths:

```java
// S(t) = S(0) * exp((μ - 0.5σ²)Δt + σ√Δt * Z)
// where Z ~ N(0,1)

for (int day = 0; day < horizon; day++) {
    double z = normalDist.sample();
    double dW = z * Math.sqrt(dt);
    
    price = price * Math.exp((drift - 0.5 * volatility * volatility) * dt + 
                            volatility * dW);
}
```

## Domain Models

### ARIMASignal
- `trend`: Raw trend value
- `trendPercentage`: Trend as percentage
- `structuralBreakDetected`: Boolean flag
- `confidence`: ARIMA confidence (0-1)
- `description`: Human-readable description
- `cusumStatistic`: CUSUM test statistic
- `threshold`: CUSUM threshold

### MomentumMetrics
- `drift`: Expected return (annualized)
- `volatility`: Standard deviation (annualized)
- `confidence`: Bayesian confidence (0-1)
- `priorMean`: Prior mean (from ARIMA)
- `posteriorMean`: Posterior mean (after update)
- `priorVariance`: Prior variance
- `posteriorVariance`: Posterior variance

### MarketPrediction
- `probabilityUp`: Probability of price increase
- `probabilityDown`: Probability of price decrease
- `probabilityNeutral`: Probability of sideways movement
- `expectedPriceChange`: Expected absolute change
- `expectedPriceChangePercent`: Expected percentage change
- `mostLikelyScenario`: Most likely outcome
- `priceTargets`: Price targets at various percentiles

### ABCAnalysisResult
- `arimaSignal`: Stage 1 output
- `momentumMetrics`: Stage 2 output
- `marketPrediction`: Stage 3 output
- `abcIntegrationConfidence`: Overall confidence
- `needsRecalibration`: Recalibration flag
- `marketRegime`: Current market regime

## References

1. **ARIMA**: Box, G. E. P., & Jenkins, G. M. (1976). Time Series Analysis: Forecasting and Control.
2. **CUSUM**: Page, E. S. (1954). Continuous Inspection Schemes. Biometrika, 41(1/2), 100-115.
3. **Bayesian Inference**: Gelman, A., et al. (2013). Bayesian Data Analysis.
4. **Monte Carlo**: Glasserman, P. (2003). Monte Carlo Methods in Financial Engineering.
5. **Geometric Brownian Motion**: Black, F., & Scholes, M. (1973). The Pricing of Options and Corporate Liabilities.
