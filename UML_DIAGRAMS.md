# Finbot UML Diagrams

## 1. System Architecture - Component Diagram

```mermaid
graph TB
    subgraph External["External Systems"]
        Binance[Binance WebSocket API]
        NewsAPI[NewsData.io API]
        OpenAI[OpenAI GPT-4]
    end
    
    subgraph Services["Finbot Services"]
        Ingestion[Ingestion Service<br/>Port 8081]
        Analytics[Analytics Service<br/>Port 8082]
        News[News Service]
        AI[AI Decision Service]
        WebSocket[WebSocket API<br/>Port 8080]
    end
    
    subgraph Infrastructure["Infrastructure"]
        Redis[(Redis<br/>Pub/Sub + KV)]
    end
    
    subgraph Frontend["Frontend"]
        Dashboard[React Dashboard<br/>Port 3000]
    end
    
    Binance -->|Market Ticks| Ingestion
    Ingestion -->|market-stream| Redis
    Redis -->|Subscribe| Analytics
    Analytics -->|latest_snapshot:{symbol}| Redis
    
    NewsAPI -->|Headlines| News
    News -->|news-stream| Redis
    
    Redis -->|NewsSignal + Snapshot| AI
    AI -->|OpenAI API| OpenAI
    OpenAI -->|Decision| AI
    AI -->|market-decisions<br/>latest_context:{symbol}| Redis
    
    Redis -->|Fetch Snapshots| WebSocket
    WebSocket -->|ws://| Dashboard
    
    style Binance fill:#e1f5ff
    style NewsAPI fill:#e1f5ff
    style OpenAI fill:#e1f5ff
    style Redis fill:#ffebee
    style Dashboard fill:#f3e5f5
```

## 2. ABC Analysis Engine - Class Diagram

```mermaid
classDiagram
    class ABCAnalyzer {
        -ArimaForecaster arimaForecaster
        -BayesianAnalyzer bayesianAnalyzer
        -MonteCarloSimulator monteCarloSimulator
        +analyze(prices, currentPrice) ABCAnalysisResult
        -performARIMAAnalysis() ARIMASignal
        -performBayesianMomentumAnalysis() MomentumMetrics
        -performMonteCarloSimulation() MarketPrediction
        -calculateIntegrationConfidence() double
        -determineMarketRegime() String
    }
    
    class ArimaForecaster {
        -int defaultHorizon
        +forecast(prices, horizon) ArimaForecast
        +forecastReactive(prices) Uni~ArimaForecast~
        -calculateTrend() double
        -detectStructuralBreak() double
        -calculateAIC() double
    }
    
    class BayesianAnalyzer {
        +analyze(prices) BayesianMetrics
        +analyzeReactive(prices) Uni~BayesianMetrics~
        -calculateLogReturns() double[]
        -updatePosterior() void
    }
    
    class MonteCarloSimulator {
        -int defaultSimulations
        -int defaultHorizon
        +simulate(price, drift, volatility) MonteCarloResults
        +simulateReactive() Uni~MonteCarloResults~
        -runGBMSimulation() double[]
        -calculateVaR() double
        -calculateCVaR() double
    }
    
    class ABCAnalysisResult {
        +ARIMASignal arimaSignal
        +MomentumMetrics momentumMetrics
        +MarketPrediction marketPrediction
        +BigDecimal abcIntegrationConfidence
        +boolean needsRecalibration
        +String marketRegime
    }
    
    class ARIMASignal {
        +BigDecimal trend
        +BigDecimal trendPercentage
        +boolean structuralBreakDetected
        +BigDecimal confidence
        +String description
    }
    
    class BayesianMetrics {
        +BigDecimal drift
        +BigDecimal volatility
        +BigDecimal confidence
        +int sampleSize
    }
    
    class MonteCarloResults {
        +int simulations
        +BigDecimal probabilityUp
        +BigDecimal probabilityDown
        +BigDecimal expectedReturn
        +BigDecimal valueAtRisk95
        +BigDecimal valueAtRisk99
        +BigDecimal conditionalVaR
    }
    
    ABCAnalyzer --> ArimaForecaster
    ABCAnalyzer --> BayesianAnalyzer
    ABCAnalyzer --> MonteCarloSimulator
    ABCAnalyzer ..> ABCAnalysisResult : creates
    ArimaForecaster ..> ARIMASignal : creates
    BayesianAnalyzer ..> BayesianMetrics : creates
    MonteCarloSimulator ..> MonteCarloResults : creates
    ABCAnalysisResult *-- ARIMASignal
    ABCAnalysisResult *-- BayesianMetrics
    ABCAnalysisResult *-- MonteCarloResults
```

## 3. Data Flow - Sequence Diagram

```mermaid
sequenceDiagram
    participant Binance as Binance API
    participant Ingestion as Ingestion Service
    participant Redis as Redis
    participant Analytics as Analytics Service
    participant News as News Service
    participant AI as AI Decision Service
    participant WS as WebSocket API
    participant Dashboard as Dashboard
    
    Note over Binance,Dashboard: Real-time Market Data Flow
    
    Binance->>Ingestion: WebSocket: Trade Event
    Ingestion->>Ingestion: Normalize to MarketTick
    Ingestion->>Redis: PUBLISH market-stream
    
    Redis->>Analytics: SUBSCRIBE market-stream
    Analytics->>Analytics: Add to sliding window (500 ticks)
    
    loop Every 5 seconds
        Analytics->>Analytics: Generate Snapshot
        Note over Analytics: ABC Analysis Pipeline
        Analytics->>Analytics: 1. ARIMA Analysis
        Analytics->>Analytics: 2. Bayesian Analysis (ARIMA-informed)
        Analytics->>Analytics: 3. Monte Carlo Simulation
        Analytics->>Analytics: Integrate Results
        Analytics->>Redis: SET latest_snapshot:BTC
    end
    
    loop Every 5 minutes
        News->>NewsAPI: Fetch Headlines
        NewsAPI-->>News: Articles
        News->>News: Calculate Sentiment
        News->>Redis: PUBLISH news-stream
    end
    
    Redis->>AI: SUBSCRIBE news-stream
    AI->>Redis: GET latest_snapshot:BTC
    Redis-->>AI: MarketSnapshot
    AI->>OpenAI: Generate Decision (GPT-4)
    OpenAI-->>AI: Signal + Confidence + Reasoning
    AI->>Redis: PUBLISH market-decisions
    AI->>Redis: SET latest_context:BTC
    
    loop Every 1 second
        WS->>Redis: GET latest_snapshot:BTC
        WS->>Redis: GET latest_context:BTC
        Redis-->>WS: Snapshot + Context
        WS->>Dashboard: WebSocket Broadcast
        Dashboard->>Dashboard: Update UI
    end
```

## 4. ABC Analysis Pipeline - Activity Diagram

```mermaid
flowchart TD
    Start([Receive Market Ticks]) --> Window[Add to Sliding Window<br/>Max 500 ticks]
    Window --> Check{Window Size<br/>>= 30?}
    Check -->|No| Wait[Wait for more data]
    Wait --> Start
    Check -->|Yes| Extract[Extract Price Series]
    
    Extract --> ARIMA[ARIMA Analysis]
    
    subgraph ARIMA_Stage["Stage 1: ARIMA"]
        ARIMA --> Smooth[Exponential Smoothing<br/>Holt's Method]
        Smooth --> Trend[Calculate Trend]
        Trend --> CUSUM[CUSUM Structural<br/>Break Detection]
        CUSUM --> ARIMAOut[ARIMASignal Output]
    end
    
    ARIMAOut --> Bayesian[Bayesian Analysis]
    
    subgraph Bayesian_Stage["Stage 2: Bayesian"]
        Bayesian --> LogRet[Calculate Log Returns]
        LogRet --> Prior[Set ARIMA-informed Prior]
        Prior --> Update[Bayesian Update]
        Update --> Annualize[Annualize Drift & Volatility]
        Annualize --> BayesOut[BayesianMetrics Output]
    end
    
    BayesOut --> MonteCarlo[Monte Carlo Simulation]
    
    subgraph MC_Stage["Stage 3: Monte Carlo"]
        MonteCarlo --> GBM[Run 10,000 GBM Simulations]
        GBM --> Stats[Calculate Statistics]
        Stats --> Risk[Calculate VaR & CVaR]
        Risk --> MCOut[MonteCarloResults Output]
    end
    
    MCOut --> Integrate[Calculate Integration Confidence]
    Integrate --> Regime[Determine Market Regime]
    Regime --> Recal{Structural<br/>Break or<br/>High Vol?}
    Recal -->|Yes| Flag[Flag Recalibration Needed]
    Recal -->|No| NoFlag[No Recalibration]
    Flag --> Result[ABCAnalysisResult]
    NoFlag --> Result
    Result --> Save[Save to Redis]
    Save --> End([Snapshot Complete])
    
    style ARIMA_Stage fill:#e3f2fd
    style Bayesian_Stage fill:#e8f5e9
    style MC_Stage fill:#fff3e0
```

## 5. Service Integration - Deployment Diagram

```mermaid
graph TB
    subgraph Docker["Docker Compose Environment"]
        subgraph Container1["ingestion-service"]
            Ing[Ingestion Service<br/>Java 21 + Quarkus]
        end
        
        subgraph Container2["analytics-service"]
            Ana[Analytics Service<br/>Java 21 + Quarkus<br/>ABC Engine]
        end
        
        subgraph Container3["news-service"]
            News[News Service<br/>Java 21 + Quarkus]
        end
        
        subgraph Container4["ai-decision-service"]
            AI[AI Decision Service<br/>Java 21 + Quarkus]
        end
        
        subgraph Container5["websocket-api"]
            WS[WebSocket API<br/>Java 21 + Quarkus]
        end
        
        subgraph Container6["dashboard"]
            Dash[React Dashboard<br/>Nginx]
        end
        
        subgraph Container7["redis"]
            Redis[(Redis 7<br/>Pub/Sub + KV)]
        end
    end
    
    Ing -->|market-stream| Redis
    Redis -->|Subscribe| Ana
    Ana -->|latest_snapshot:{symbol}| Redis
    News -->|news-stream| Redis
    Redis -->|Subscribe| AI
    AI -->|latest_context:{symbol}| Redis
    Redis -->|Fetch| WS
    WS -->|ws://8080| Dash
    
    Ing -.->|Health Check| Container1
    Ana -.->|Health Check| Container2
    News -.->|Health Check| Container3
    AI -.->|Health Check| Container4
    WS -.->|Health Check| Container5
    
    style Redis fill:#ffcdd2
    style Dash fill:#c5cae9
```

## 6. Domain Model - Class Diagram

```mermaid
classDiagram
    class MarketTick {
        +String symbol
        +BigDecimal price
        +BigDecimal volume
        +Instant timestamp
        +String exchange
    }
    
    class MarketSnapshot {
        +String symbol
        +Instant timestamp
        +BigDecimal currentPrice
        +BayesianMetrics bayesianMetrics
        +ArimaForecast arimaForecast
        +MonteCarloResults monteCarloResults
        +String marketState
        +ABCAnalysisResult abcAnalysis
    }
    
    class NewsSignal {
        +String symbol
        +BigDecimal sentimentScore
        +int newsVolume
        +Instant timestamp
        +List~String~ headlines
    }
    
    class MarketDecision {
        +String symbol
        +String signal
        +BigDecimal confidence
        +String reasoning
        +Instant timestamp
    }
    
    class MarketContext {
        +String symbol
        +BigDecimal price
        +String trend
        +BigDecimal volatility
        +BigDecimal newsSentiment
        +int newsVolume
        +String aiSignal
        +BigDecimal aiConfidence
        +String aiReasoning
        +String decision
        +Instant timestamp
    }
    
    class ABCAnalysisResult {
        +ARIMASignal arimaSignal
        +MomentumMetrics momentumMetrics
        +MarketPrediction marketPrediction
        +BigDecimal abcIntegrationConfidence
        +boolean needsRecalibration
        +String marketRegime
    }
    
    class ARIMASignal {
        +BigDecimal trend
        +BigDecimal trendPercentage
        +boolean structuralBreakDetected
        +BigDecimal confidence
        +String description
    }
    
    class MomentumMetrics {
        +BigDecimal drift
        +BigDecimal volatility
        +BigDecimal confidence
        +BigDecimal priorMean
        +BigDecimal posteriorMean
    }
    
    class MarketPrediction {
        +BigDecimal probabilityUp
        +BigDecimal probabilityDown
        +BigDecimal probabilityNeutral
        +BigDecimal expectedPriceChange
        +String mostLikelyScenario
        +List~PriceTarget~ priceTargets
    }
    
    MarketTick --> MarketSnapshot : aggregated into
    MarketSnapshot *-- ABCAnalysisResult
    ABCAnalysisResult *-- ARIMASignal
    ABCAnalysisResult *-- MomentumMetrics
    ABCAnalysisResult *-- MarketPrediction
    MarketSnapshot --> MarketDecision : combined with
    NewsSignal --> MarketDecision : influences
    MarketSnapshot --> MarketContext : creates
    NewsSignal --> MarketContext : contributes to
    MarketDecision --> MarketContext : included in
```

## 7. Reactive Processing - Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Client Request
    participant Service as MarketAnalysisService
    participant Bayes as BayesianAnalyzer
    participant ARIMA as ArimaForecaster
    participant MC as MonteCarloSimulator
    participant VT1 as Virtual Thread 1
    participant VT2 as Virtual Thread 2
    participant VT3 as Virtual Thread 3
    
    Note over Client,VT3: Reactive Parallel Analysis (Java 21)
    
    Client->>Service: generateSnapshotReactive("BTC")
    Service->>Service: Extract price window
    
    par Parallel Execution
        Service->>Bayes: analyzeReactive(prices)
        Bayes->>VT1: Execute on Virtual Thread
        VT1->>VT1: Calculate log returns
        VT1->>VT1: Bayesian update
        VT1->>VT1: Annualize metrics
        VT1-->>Bayes: BayesianMetrics
        
        Service->>ARIMA: forecastReactive(prices)
        ARIMA->>VT2: Execute on Virtual Thread
        VT2->>VT2: Exponential smoothing
        VT2->>VT2: Trend calculation
        VT2->>VT2: CUSUM detection
        VT2-->>ARIMA: ArimaForecast
    end
    
    Bayes-->>Service: Uni~BayesianMetrics~
    Service->>MC: simulateReactive(price, drift, vol)
    MC->>VT3: Execute on Virtual Thread
    VT3->>VT3: 10,000 GBM simulations
    VT3->>VT3: Calculate VaR/CVaR
    VT3-->>MC: MonteCarloResults
    
    ARIMA-->>Service: Uni~ArimaForecast~
    MC-->>Service: Uni~MonteCarloResults~
    
    Service->>Service: Combine all results
    Service->>Service: Create MarketSnapshot
    Service-->>Client: Uni~MarketSnapshot~
    
    Note over VT1,VT3: Virtual Threads: ~1KB each<br/>Platform Threads: ~1MB each<br/>Enables 1M+ concurrent operations
```

## 8. Hexagonal Architecture - Component Diagram

```mermaid
graph TB
    subgraph Domain["Domain Layer (Pure Business Logic)"]
        ABCAnalyzer[ABCAnalyzer]
        BayesianAnalyzer[BayesianAnalyzer]
        ArimaForecaster[ArimaForecaster]
        MonteCarloSimulator[MonteCarloSimulator]
        DecisionEngine[DecisionEngine]
    end
    
    subgraph Application["Application Layer (Use Cases)"]
        MarketAnalysisService[MarketAnalysisService]
        IngestionService[IngestionService]
        NewsPollingService[NewsPollingService]
        BroadcastService[BroadcastService]
    end
    
    subgraph Ports["Ports (Interfaces)"]
        AnalysisService{{AnalysisService}}
        SnapshotRepository{{SnapshotRepository}}
        MarketDataPublisher{{MarketDataPublisher}}
        NewsRepository{{NewsRepository}}
    end
    
    subgraph Adapters["Infrastructure Adapters"]
        BinanceWSClient[BinanceWebSocketClient]
        RedisPublisher[RedisMarketDataPublisher]
        RedisSnapshot[RedisSnapshotRepository]
        NewsApiClient[NewsApiClient]
        RedisNewsPublisher[RedisNewsPublisher]
        WebSocketBroadcaster[WebSocketBroadcaster]
    end
    
    subgraph External["External Systems"]
        Binance[(Binance API)]
        Redis[(Redis)]
        NewsAPI[(NewsData.io)]
        Browser[Browser/Dashboard]
    end
    
    MarketAnalysisService --> ABCAnalyzer
    ABCAnalyzer --> BayesianAnalyzer
    ABCAnalyzer --> ArimaForecaster
    ABCAnalyzer --> MonteCarloSimulator
    
    MarketAnalysisService -.implements.-> AnalysisService
    MarketAnalysisService --> SnapshotRepository
    IngestionService --> MarketDataPublisher
    NewsPollingService --> NewsRepository
    
    RedisSnapshot -.implements.-> SnapshotRepository
    RedisPublisher -.implements.-> MarketDataPublisher
    RedisNewsPublisher -.implements.-> NewsRepository
    
    BinanceWSClient --> Binance
    RedisPublisher --> Redis
    RedisSnapshot --> Redis
    NewsApiClient --> NewsAPI
    RedisNewsPublisher --> Redis
    WebSocketBroadcaster --> Browser
    
    IngestionService --> BinanceWSClient
    IngestionService --> RedisPublisher
    NewsPollingService --> NewsApiClient
    NewsPollingService --> RedisNewsPublisher
    BroadcastService --> RedisSnapshot
    BroadcastService --> WebSocketBroadcaster
    
    style Domain fill:#e8f5e9
    style Application fill:#e3f2fd
    style Ports fill:#fff3e0
    style Adapters fill:#f3e5f5
    style External fill:#ffebee
```

## 9. State Machine - Market Regime Transitions

```mermaid
stateDiagram-v2
    [*] --> NEUTRAL_STABLE
    
    NEUTRAL_STABLE --> BULLISH_STABLE: Positive drift<br/>Low volatility<br/>Prob Up > 60%
    NEUTRAL_STABLE --> BEARISH_STABLE: Negative drift<br/>Low volatility<br/>Prob Up < 40%
    NEUTRAL_STABLE --> NEUTRAL_VOLATILE: Volatility > 30%
    
    BULLISH_STABLE --> BULLISH_VOLATILE: Volatility > 30%
    BULLISH_STABLE --> NEUTRAL_STABLE: Drift → 0<br/>Prob Up → 50%
    BULLISH_STABLE --> REGIME_CHANGE: Structural Break
    
    BEARISH_STABLE --> BEARISH_VOLATILE: Volatility > 30%
    BEARISH_STABLE --> NEUTRAL_STABLE: Drift → 0<br/>Prob Up → 50%
    BEARISH_STABLE --> REGIME_CHANGE: Structural Break
    
    BULLISH_VOLATILE --> BULLISH_STABLE: Volatility < 30%
    BULLISH_VOLATILE --> REGIME_CHANGE: Structural Break
    
    BEARISH_VOLATILE --> BEARISH_STABLE: Volatility < 30%
    BEARISH_VOLATILE --> REGIME_CHANGE: Structural Break
    
    NEUTRAL_VOLATILE --> NEUTRAL_STABLE: Volatility < 30%
    NEUTRAL_VOLATILE --> REGIME_CHANGE: Structural Break
    
    REGIME_CHANGE --> HIGH_VOLATILITY: Volatility > 50%
    REGIME_CHANGE --> NEUTRAL_STABLE: Recalibration Complete
    
    HIGH_VOLATILITY --> NEUTRAL_VOLATILE: Volatility < 50%
    HIGH_VOLATILITY --> REGIME_CHANGE: Structural Break
    
    note right of REGIME_CHANGE
        Triggers:
        - CUSUM > threshold
        - Confidence reduced 30%
        - Recalibration needed
    end note
    
    note right of HIGH_VOLATILITY
        Risk State:
        - Volatility > 50%
        - Increased uncertainty
        - Conservative decisions
    end note
```

## 10. Performance Optimization - Component Interaction

```mermaid
graph LR
    subgraph Request["Client Request"]
        Req[Generate Snapshot]
    end
    
    subgraph Reactive["Reactive Pipeline (Mutiny)"]
        Uni1[Uni~Bayesian~]
        Uni2[Uni~ARIMA~]
        Uni3[Uni~MonteCarlo~]
        Combine[Uni.combine.all]
    end
    
    subgraph VirtualThreads["Java 21 Virtual Threads"]
        VT1[VT Pool<br/>Bayesian]
        VT2[VT Pool<br/>ARIMA]
        VT3[VT Pool<br/>Monte Carlo]
    end
    
    subgraph NonBlocking["Non-blocking I/O"]
        RedisRead[Redis Read<br/>Reactive]
        RedisWrite[Redis Write<br/>Reactive]
    end
    
    subgraph Result["Response"]
        Snapshot[MarketSnapshot]
    end
    
    Req --> Uni1
    Req --> Uni2
    Uni1 --> VT1
    Uni2 --> VT2
    
    VT1 --> Combine
    VT2 --> Combine
    Combine --> Uni3
    Uni3 --> VT3
    VT3 --> Combine
    
    Combine --> RedisWrite
    RedisWrite --> Snapshot
    
    RedisRead -.->|Non-blocking| Req
    
    style VirtualThreads fill:#e8f5e9
    style Reactive fill:#e3f2fd
    style NonBlocking fill:#fff3e0
    
    Note1[Latency: 5ms<br/>Throughput: 50K/sec<br/>Memory: 50MB]
    Note1 -.-> Result
```

## Diagram Usage Guide

### For Developers
- **Diagram 2 (Class)**: Understand ABC engine structure
- **Diagram 3 (Sequence)**: Trace data flow through system
- **Diagram 7 (Reactive)**: Learn parallel processing patterns
- **Diagram 8 (Hexagonal)**: Navigate codebase architecture

### For Architects
- **Diagram 1 (Component)**: System overview and integration points
- **Diagram 5 (Deployment)**: Container orchestration
- **Diagram 8 (Hexagonal)**: Architectural patterns and boundaries

### For Stakeholders
- **Diagram 4 (Activity)**: ABC analysis workflow
- **Diagram 9 (State)**: Market regime classification logic
- **Diagram 3 (Sequence)**: End-to-end data processing

### For Data Scientists
- **Diagram 2 (Class)**: Algorithm implementations
- **Diagram 4 (Activity)**: Analysis pipeline stages
- **Diagram 6 (Domain)**: Data model relationships

---

## Rendering Instructions

These diagrams use **Mermaid** syntax. To render them:

### In GitHub/GitLab
Diagrams render automatically in markdown files.

### In VS Code
Install the "Markdown Preview Mermaid Support" extension.

### Online
Copy diagram code to: https://mermaid.live/

### In Documentation Sites
Most modern documentation generators (MkDocs, Docusaurus, etc.) support Mermaid natively.
