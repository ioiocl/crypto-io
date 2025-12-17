# Finbot - Real-time Financial Analysis Platform

A comprehensive financial analysis platform built with **Java 21**, **Quarkus**, and **Hexagonal Architecture**, featuring real-time market data ingestion, advanced analytics (Bayesian, ARIMA, Monte Carlo), and a modern React dashboard.

## 🏗️ Architecture

### Hexagonal Architecture (Ports & Adapters)

The system follows hexagonal architecture principles with clear separation between:
- **Domain Layer**: Pure business logic (Bayesian, ARIMA, Monte Carlo analyzers)
- **Application Layer**: Use case orchestration
- **Infrastructure Layer**: Adapters for external systems (Redis, WebSocket, etc.)

### Services

1. **Ingestion Service** (Port 8081)
   - WebSocket client to Massive/Polygon API
   - Normalizes market data to domain models
   - Publishes to Redis Pub/Sub

2. **Analytics Service** (Port 8082)
   - Subscribes to market data stream
   - Performs Bayesian, ARIMA, and Monte Carlo analysis
   - Stores snapshots in Redis

3. **WebSocket API** (Port 8080)
   - Real-time WebSocket server
   - Broadcasts market snapshots to clients
   - Stateless and horizontally scalable

4. **Dashboard** (Port 3000)
   - React + TailwindCSS + Lightweight Charts
   - Real-time data visualization
   - Modern, responsive UI

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 20+ (for dashboard development)
- Maven 3.9+ (for building)

### Local Deployment

1. **Clone and configure**:
```bash
cd C:\Users\avasquezp\Documents\tmp\Finbot
cp .env.example .env
# Edit .env with your Polygon API key
```

2. **Build and run**:
```bash
docker compose up --build
```

3. **Access the application**:
   - Dashboard: http://localhost:3000
   - WebSocket API: ws://localhost:8080/ws/market/{symbol}
   - Redis: localhost:6379

### Environment Variables

Create a `.env` file with:

```env
# Massive API (formerly Polygon)
POLYGON_API_KEY=LkgydUcNGAFPthknFLbtkvshslkuSNqU
# Use delayed feed (free) or realtime feed (requires subscription)
POLYGON_WEBSOCKET_URL=wss://delayed.massive.com/v1/stocks
POLYGON_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN

# Analytics
ANALYTICS_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
SNAPSHOT_INTERVAL=5s

# WebSocket API
BROADCAST_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
BROADCAST_INTERVAL=1s

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
```

## 📊 Analytics Features

### Bayesian Analysis
- Estimates drift (μ) and volatility (σ)
- Uses conjugate priors for normal distribution
- Provides confidence intervals
- Annualized metrics (252 trading days)

### ARIMA Forecasting
- Simplified ARIMA(1,1,1) with exponential smoothing
- Generates price predictions with confidence intervals
- Calculates AIC for model quality
- Configurable forecast horizon

### Monte Carlo Simulation
- Geometric Brownian Motion (GBM)
- 10,000 simulations by default
- Calculates:
  - Probability of price increase/decrease
  - Value at Risk (VaR) at 95% and 99%
  - Conditional VaR (CVaR/Expected Shortfall)
  - Percentile distributions

## 🏢 Cloud Deployment

### Alibaba Cloud (ECS)

1. **Navigate to Terraform directory**:
```bash
cd terraform/alibaba
```

2. **Initialize and configure**:
```bash
terraform init

# Create terraform.tfvars
cat > terraform.tfvars <<EOF
access_key = "YOUR_ACCESS_KEY"
secret_key = "YOUR_SECRET_KEY"
region = "us-west-1"
redis_password = "YOUR_REDIS_PASSWORD"
polygon_api_key = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
EOF
```

3. **Deploy**:
```bash
terraform plan
terraform apply
```

4. **Build and push images**:
```bash
# Get registry URL from terraform output
REGISTRY=$(terraform output -raw container_registry_url)

# Build and push
cd ../..
docker build -t $REGISTRY/ingestion-service:latest -f ingestion-service/Dockerfile .
docker push $REGISTRY/ingestion-service:latest

docker build -t $REGISTRY/analytics-service:latest -f analytics-service/Dockerfile .
docker push $REGISTRY/analytics-service:latest

docker build -t $REGISTRY/websocket-api:latest -f websocket-api/Dockerfile .
docker push $REGISTRY/websocket-api:latest

docker build -t $REGISTRY/dashboard:latest -f dashboard/Dockerfile ./dashboard
docker push $REGISTRY/dashboard:latest
```

### Google Cloud Platform (GCP)

#### Option 1: GCE (Compute Engine)

1. **Navigate to Terraform directory**:
```bash
cd terraform/gcp
```

2. **Initialize and configure**:
```bash
terraform init

# Create terraform.tfvars
cat > terraform.tfvars <<EOF
project_id = "your-gcp-project-id"
region = "us-central1"
polygon_api_key = "LkgydUcNGAFPthknFLbtkvshslkuSNqU"
EOF
```

3. **Deploy**:
```bash
terraform plan
terraform apply
```

4. **Build and push images to Artifact Registry**:
```bash
# Authenticate
gcloud auth configure-docker us-central1-docker.pkg.dev

# Get registry URL
REGISTRY=$(terraform output -raw artifact_registry_url)

# Build and push
cd ../..
docker build -t $REGISTRY/ingestion-service:latest -f ingestion-service/Dockerfile .
docker push $REGISTRY/ingestion-service:latest

docker build -t $REGISTRY/analytics-service:latest -f analytics-service/Dockerfile .
docker push $REGISTRY/analytics-service:latest

docker build -t $REGISTRY/websocket-api:latest -f websocket-api/Dockerfile .
docker push $REGISTRY/websocket-api:latest

docker build -t $REGISTRY/dashboard:latest -f dashboard/Dockerfile ./dashboard
docker push $REGISTRY/dashboard:latest
```

#### Option 2: Cloud Run (Serverless)

The Terraform configuration also creates Cloud Run services. These are automatically deployed when you run `terraform apply`.

**Note**: Cloud Run services use VPC Access Connector to communicate with Memorystore Redis.

## 🔧 Development

### Building Individual Services

```bash
# Build all services
mvn clean package

# Build specific service
mvn -f ingestion-service/pom.xml clean package
mvn -f analytics-service/pom.xml clean package
mvn -f websocket-api/pom.xml clean package
```

### Running Services Locally

```bash
# Ingestion Service
cd ingestion-service
mvn quarkus:dev

# Analytics Service
cd analytics-service
mvn quarkus:dev

# WebSocket API
cd websocket-api
mvn quarkus:dev

# Dashboard
cd dashboard
npm install
npm run dev
```

### Testing WebSocket Connection

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/market/AAPL');

ws.onopen = () => console.log('Connected');
ws.onmessage = (event) => console.log('Data:', JSON.parse(event.data));
ws.onerror = (error) => console.error('Error:', error);
```

## 📁 Project Structure

```
Finbot/
├── shared-domain/              # Shared domain models and ports
│   └── src/main/java/cl/ioio/finbot/domain/
│       ├── model/              # Domain entities
│       └── ports/              # Port interfaces
├── ingestion-service/          # Market data ingestion
│   └── src/main/java/cl/ioio/finbot/ingestion/
│       ├── adapter/            # WebSocket & Redis adapters
│       └── application/        # Application services
├── analytics-service/          # Analysis engine
│   └── src/main/java/cl/ioio/finbot/analytics/
│       ├── domain/             # Bayesian, ARIMA, Monte Carlo
│       ├── adapter/            # Redis adapters
│       └── application/        # Analysis orchestration
├── websocket-api/              # Real-time API
│   └── src/main/java/cl/ioio/finbot/websocket/
│       ├── adapter/            # Redis adapter
│       └── BroadcastService    # WebSocket broadcasting
├── dashboard/                  # React frontend
│   └── src/
│       ├── components/         # React components
│       └── App.jsx             # Main application
├── terraform/
│   ├── alibaba/                # Alibaba Cloud infrastructure
│   └── gcp/                    # GCP infrastructure
└── docker-compose.yml          # Local deployment
```

## 🔐 Security Best Practices

1. **API Keys**: Never hardcode API keys. Use environment variables.
2. **Redis**: Use authentication in production (configure password).
3. **HTTPS**: Use SSL/TLS certificates for production deployments.
4. **Firewall**: Restrict access to necessary ports only.
5. **Secrets Management**: Use cloud provider secret managers (Alibaba KMS, GCP Secret Manager).

## 📈 Monitoring

### Logs

```bash
# View all service logs
docker compose logs -f

# View specific service
docker compose logs -f ingestion-service
docker compose logs -f analytics-service
docker compose logs -f websocket-api
```

### Health Checks

- Ingestion Service: http://localhost:8081/q/health
- Analytics Service: http://localhost:8082/q/health
- WebSocket API: http://localhost:8080/q/health

### Metrics

Quarkus provides built-in metrics at `/q/metrics` endpoint for each service.

## 🐛 Troubleshooting

### WebSocket Connection Issues

1. Check if WebSocket API is running: `docker compose ps`
2. Verify Redis connection: `docker compose logs redis`
3. Check firewall rules for port 8080

### No Data in Dashboard

1. Verify Polygon API key is correct
2. Check ingestion service logs: `docker compose logs ingestion-service`
3. Verify Redis Pub/Sub: `docker compose exec redis redis-cli PUBSUB CHANNELS`

### Analytics Not Updating

1. Check analytics service logs: `docker compose logs analytics-service`
2. Verify sufficient data: Minimum 30 ticks required
3. Check Redis keys: `docker compose exec redis redis-cli KEYS "latest_snapshot:*"`

## 📝 API Documentation

### WebSocket API Endpoints

- **Connect**: `ws://localhost:8080/ws/market/{symbol}`
- **Symbols**: AAPL, GOOGL, MSFT, TSLA, AMZN (configurable)
- **Message Format**: JSON with MarketSnapshot structure

### MarketSnapshot Schema

```json
{
  "symbol": "AAPL",
  "timestamp": "2024-01-15T10:30:00Z",
  "currentPrice": 185.50,
  "marketState": "BULLISH",
  "bayesianMetrics": {
    "drift": 0.15,
    "volatility": 0.25,
    "confidence": 0.95,
    "sampleSize": 100
  },
  "arimaForecast": {
    "predictions": [186.20, 187.10, 188.00],
    "horizon": 10,
    "modelOrder": "ARIMA(1,1,1)"
  },
  "monteCarloResults": {
    "simulations": 10000,
    "probabilityUp": 0.65,
    "probabilityDown": 0.35,
    "expectedReturn": 0.08,
    "valueAtRisk95": 5.50,
    "valueAtRisk99": 8.20
  }
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Follow hexagonal architecture principles
4. Write tests for new features
5. Submit a pull request

## 📄 License

Copyright © 2024 Finbot. All rights reserved.

## 🔗 Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Massive API Docs](https://massive.com/docs) (formerly Polygon.io)
  - [WebSocket Quickstart](https://massive.com/docs/websocket/quickstart)
  - [REST API Quickstart](https://massive.com/docs/rest/quickstart)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Apache Commons Math](https://commons.apache.org/proper/commons-math/)

## 📧 Support

For issues and questions:
- Create an issue on GitHub
- Check existing documentation
- Review logs for error messages
