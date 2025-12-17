# Finbot Architecture Documentation

## Overview

Finbot is a real-time financial analysis platform built using **Hexagonal Architecture** (Ports & Adapters pattern) with Java 21 and Quarkus framework.

## Architectural Principles

### Hexagonal Architecture

The system follows hexagonal architecture to achieve:
- **Independence from frameworks**: Core business logic doesn't depend on Quarkus or any framework
- **Testability**: Domain logic can be tested without external dependencies
- **Flexibility**: Easy to swap adapters (e.g., change from Redis to Kafka)
- **Maintainability**: Clear separation of concerns

### Layers

```
┌─────────────────────────────────────────────────────────┐
│                    External Systems                      │
│  (Polygon API, Redis, WebSocket Clients, Browsers)      │
└─────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                    │
│              (Adapters - Driven & Driver)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   WebSocket  │  │    Redis     │  │  WebSocket   │  │
│  │    Client    │  │   Pub/Sub    │  │    Server    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────┐
│                  Application Layer                       │
│           (Use Cases & Orchestration)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Ingestion   │  │   Analysis   │  │  Broadcast   │  │
│  │   Service    │  │   Service    │  │   Service    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│              (Business Logic & Ports)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Bayesian   │  │    ARIMA     │  │ Monte Carlo  │  │
│  │   Analyzer   │  │  Forecaster  │  │  Simulator   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │ MarketTick   │  │   Snapshot   │  (Domain Models)   │
│  └──────────────┘  └──────────────┘                    │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │   Ports      │  │   Ports      │  (Interfaces)      │
│  │  (Input)     │  │  (Output)    │                    │
│  └──────────────┘  └──────────────┘                    │
└─────────────────────────────────────────────────────────┘
```

## Service Architecture

### 1. Ingestion Service

**Purpose**: Consume real-time market data from Massive/Polygon API

**Components**:
- **Domain**: `MarketDataIngestionPort` interface
- **Application**: `IngestionService` orchestrates ingestion
- **Adapters**:
  - `MassiveWebSocketClient` (Driver) - connects to Polygon
  - `RedisMarketDataPublisher` (Driven) - publishes to Redis

**Flow**:
```
Polygon API → WebSocket Client → Domain Model → Redis Pub/Sub
```

**Key Features**:
- Auto-reconnection on connection loss
- Message normalization to domain models
- Configurable symbols subscription
- Error handling and logging

### 2. Analytics Service

**Purpose**: Perform statistical analysis on market data

**Components**:
- **Domain**:
  - `BayesianAnalyzer` - drift and volatility estimation
  - `ArimaForecaster` - time series prediction
  - `MonteCarloSimulator` - risk assessment
- **Application**: `MarketAnalysisService` orchestrates analysis
- **Adapters**:
  - `RedisMarketDataSubscriber` (Driver) - subscribes to ticks
  - `RedisSnapshotRepository` (Driven) - stores results

**Flow**:
```
Redis Pub/Sub → Process Tick → Analyze → Generate Snapshot → Redis Key-Value
```

**Analysis Pipeline**:
1. **Data Collection**: Maintain sliding window of ticks (max 500)
2. **Bayesian Analysis**: Calculate drift (μ) and volatility (σ)
3. **ARIMA Forecast**: Generate price predictions
4. **Monte Carlo**: Run 10,000 simulations for risk metrics
5. **State Determination**: Classify as BULLISH/BEARISH/NEUTRAL
6. **Snapshot Creation**: Combine all results
7. **Persistence**: Store in Redis

### 3. WebSocket API Service

**Purpose**: Stream real-time data to web clients

**Components**:
- **Application**: `BroadcastService` manages periodic updates
- **Adapters**:
  - `MarketDataWebSocket` (Driver) - WebSocket server endpoint
  - `RedisSnapshotRepository` (Driven) - reads snapshots

**Flow**:
```
Redis Key-Value → Read Snapshot → WebSocket Broadcast → Clients
```

**Features**:
- Per-symbol WebSocket endpoints
- Automatic broadcasting (1s interval)
- Connection management
- Error handling

### 4. Dashboard Service

**Purpose**: Visualize market data in real-time

**Technology Stack**:
- React 18
- TailwindCSS for styling
- Lightweight Charts for price visualization
- Lucide React for icons

**Components**:
- `App.jsx` - Main application with symbol selector
- `SymbolCard.jsx` - Detailed view with charts and metrics
- WebSocket client for real-time updates

**Features**:
- Multi-symbol monitoring
- Real-time chart updates
- Risk metrics display
- Responsive design
- Auto-reconnection

## Data Flow

### End-to-End Flow

```
┌──────────────┐
│  Polygon API │
└──────┬───────┘
       │ WebSocket
       ↓
┌──────────────────┐
│ Ingestion Service│
└──────┬───────────┘
       │ Redis Pub/Sub (market-stream)
       ↓
┌──────────────────┐
│Analytics Service │
│ • Bayesian       │
│ • ARIMA          │
│ • Monte Carlo    │
└──────┬───────────┘
       │ Redis Key-Value (latest_snapshot:SYMBOL)
       ↓
┌──────────────────┐
│ WebSocket API    │
└──────┬───────────┘
       │ WebSocket
       ↓
┌──────────────────┐
│   Dashboard      │
└──────────────────┘
```

### Message Formats

**MarketTick** (Redis Pub/Sub):
```json
{
  "symbol": "AAPL",
  "price": 185.50,
  "volume": 1000000,
  "timestamp": "2024-01-15T10:30:00Z",
  "exchange": "NASDAQ",
  "bid": 185.48,
  "ask": 185.52
}
```

**MarketSnapshot** (Redis Key-Value & WebSocket):
```json
{
  "symbol": "AAPL",
  "timestamp": "2024-01-15T10:30:00Z",
  "currentPrice": 185.50,
  "marketState": "BULLISH",
  "bayesianMetrics": { ... },
  "arimaForecast": { ... },
  "monteCarloResults": { ... }
}
```

## Technology Stack

### Backend
- **Language**: Java 21
- **Framework**: Quarkus 3.6.4
- **Build Tool**: Maven 3.9+
- **Message Bus**: Redis Pub/Sub
- **State Storage**: Redis Key-Value
- **WebSocket**: Jakarta WebSocket API

### Frontend
- **Framework**: React 18
- **Build Tool**: Vite
- **Styling**: TailwindCSS
- **Charts**: Lightweight Charts
- **Icons**: Lucide React

### Infrastructure
- **Containerization**: Docker
- **Orchestration**: Docker Compose (local), ECS/GKE (cloud)
- **IaC**: Terraform
- **Cloud Providers**: Alibaba Cloud, GCP

## Scalability Considerations

### Horizontal Scaling

**Ingestion Service**:
- Single instance per symbol set (WebSocket connection)
- Can run multiple instances with different symbols
- Stateless after connection establishment

**Analytics Service**:
- Multiple instances possible
- Each maintains independent time windows
- Redis ensures snapshot consistency

**WebSocket API**:
- Fully stateless
- Horizontally scalable
- Load balancer distributes connections

**Dashboard**:
- Static assets served by CDN
- No server-side state

### Vertical Scaling

- Increase CPU for analytics computations
- Increase memory for larger time windows
- Redis memory for more symbols/history

### Performance Optimizations

1. **Caching**: Redis stores latest snapshots
2. **Async Processing**: Non-blocking I/O with Quarkus
3. **Batch Operations**: Periodic snapshot generation
4. **Connection Pooling**: Redis connection management
5. **Lazy Loading**: Dashboard loads data on demand

## Security Architecture

### API Key Management
- Environment variables only
- Never hardcoded
- Cloud secret managers in production

### Network Security
- VPC isolation in cloud deployments
- Security groups/firewall rules
- Private Redis connectivity
- HTTPS/WSS in production

### Authentication & Authorization
- WebSocket API: Can add token-based auth
- Dashboard: Can integrate OAuth2/OIDC
- Redis: Password authentication

## Monitoring & Observability

### Logging
- Structured logging with SLF4J
- Log levels: DEBUG, INFO, WARN, ERROR
- Centralized logging (Cloud Logging, ELK)

### Metrics
- Quarkus built-in metrics (`/q/metrics`)
- Custom business metrics
- Prometheus-compatible format

### Health Checks
- Liveness: `/q/health/live`
- Readiness: `/q/health/ready`
- Custom health indicators

### Tracing
- Distributed tracing with OpenTelemetry
- Request correlation IDs
- Performance profiling

## Disaster Recovery

### Backup Strategy
- Redis snapshots (RDB/AOF)
- Configuration backups
- Infrastructure as Code (Terraform)

### High Availability
- Multi-AZ deployment
- Redis replication
- Load balancer health checks
- Auto-restart policies

### Failover
- Automatic reconnection (WebSocket)
- Circuit breakers
- Graceful degradation
- Error boundaries

## Testing Strategy

### Unit Tests
- Domain logic (Bayesian, ARIMA, Monte Carlo)
- Pure functions without dependencies
- Mock external adapters

### Integration Tests
- Redis integration
- WebSocket communication
- End-to-end message flow

### Performance Tests
- Load testing with JMeter/Gatling
- WebSocket connection limits
- Analytics throughput
- Memory profiling

## Future Enhancements

### Technical
- [ ] Kafka for higher throughput
- [ ] PostgreSQL for historical data
- [ ] GraphQL API
- [ ] gRPC for inter-service communication
- [ ] Kubernetes deployment
- [ ] Service mesh (Istio)

### Features
- [ ] More symbols support
- [ ] Custom alerts
- [ ] Portfolio tracking
- [ ] Backtesting engine
- [ ] Machine learning models
- [ ] Social sentiment analysis

### Analytics
- [ ] GARCH models for volatility
- [ ] Kalman filters
- [ ] Deep learning (LSTM)
- [ ] Options pricing
- [ ] Risk parity optimization

## References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Quarkus Documentation](https://quarkus.io/guides/)
- [Apache Commons Math](https://commons.apache.org/proper/commons-math/)
