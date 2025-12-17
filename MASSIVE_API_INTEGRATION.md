# Massive.com API Integration Guide

This document explains how Finbot integrates with the Massive.com API (formerly Polygon.io).

## API Overview

Massive.com provides real-time and delayed market data through WebSocket and REST APIs.

**Documentation**: https://massive.com/docs

## WebSocket Integration

### Endpoints

Finbot uses the WebSocket API for real-time data streaming:

- **Delayed (15-min)**: `wss://delayed.massive.com/v1/stocks` (Free tier)
- **Real-time**: `wss://realtime.massive.com/v1/stocks` (Requires subscription)

### Connection Flow

1. **Connect** to WebSocket endpoint
2. **Authenticate** with API key
3. **Subscribe** to data feeds
4. **Receive** streaming data

### Authentication

After connecting, send authentication message:

```json
{
  "action": "auth",
  "key": "YOUR_API_KEY"
}
```

**Response** (success):
```json
{
  "status": "authenticated",
  "message": "authenticated"
}
```

### Subscription

Subscribe to aggregate minute bars (AM) for symbols:

```json
{
  "action": "subscribe",
  "params": "AM.AAPL,AM.MSFT,AM.GOOGL"
}
```

**Available channels**:
- `T.{symbol}` - Trades
- `Q.{symbol}` - Quotes
- `A.{symbol}` - Aggregate (second)
- `AM.{symbol}` - Aggregate (minute) ← **Used by Finbot**

### Message Format

**Aggregate Minute (AM) Event**:
```json
{
  "ev": "AM",
  "sym": "AAPL",
  "v": 12345,
  "o": 185.50,
  "c": 186.20,
  "h": 186.50,
  "l": 185.30,
  "a": 185.90,
  "s": 1705320000000,
  "e": 1705320060000
}
```

**Field Descriptions**:
- `ev`: Event type ("AM" = Aggregate Minute)
- `sym`: Symbol (e.g., "AAPL")
- `v`: Volume
- `o`: Open price
- `c`: Close price
- `h`: High price
- `l`: Low price
- `a`: VWAP (Volume Weighted Average Price)
- `s`: Start timestamp (Unix milliseconds)
- `e`: End timestamp (Unix milliseconds)

**Multiple Events**:
Massive.com may send multiple events in a single message as a JSON array:

```json
[
  {"ev": "AM", "sym": "AAPL", ...},
  {"ev": "AM", "sym": "MSFT", ...},
  {"ev": "AM", "sym": "GOOGL", ...}
]
```

## Implementation in Finbot

### MassiveWebSocketClient

Located: `ingestion-service/src/main/java/cl/ioio/finbot/ingestion/adapter/MassiveWebSocketClient.java`

**Key Features**:
1. ✅ Connects to Massive.com WebSocket
2. ✅ Authenticates with API key
3. ✅ Subscribes to AM (Aggregate Minute) feeds
4. ✅ Handles single and array message formats
5. ✅ Normalizes to domain `MarketTick` model
6. ✅ Auto-reconnection on disconnect
7. ✅ Error handling and logging

**Message Processing**:
```java
// Handles both single events and arrays
if (message.startsWith("[")) {
    JsonNode events = objectMapper.readTree(message);
    for (JsonNode event : events) {
        processEvent(event);
    }
} else {
    JsonNode event = objectMapper.readTree(message);
    processEvent(event);
}
```

**Data Normalization**:
```java
MarketTick tick = MarketTick.builder()
    .symbol(event.get("sym").asText())
    .price(new BigDecimal(event.get("c").asText()))  // Close price
    .volume(event.get("v").asLong())
    .timestamp(Instant.ofEpochMilli(event.get("e").asLong()))
    .open(new BigDecimal(event.get("o").asText()))
    .high(new BigDecimal(event.get("h").asText()))
    .low(new BigDecimal(event.get("l").asText()))
    .exchange("MASSIVE")
    .build();
```

## Configuration

### Environment Variables

```env
# API Key (get from https://massive.com/dashboard/keys)
POLYGON_API_KEY=your_api_key_here

# WebSocket URL (choose delayed or realtime)
POLYGON_WEBSOCKET_URL=wss://delayed.massive.com/v1/stocks

# Symbols to track
POLYGON_SYMBOLS=AAPL,GOOGL,MSFT,TSLA,AMZN
```

### Application Properties

Located: `ingestion-service/src/main/resources/application.properties`

```properties
polygon.api.key=${POLYGON_API_KEY:LkgydUcNGAFPthknFLbtkvshslkuSNqU}
polygon.websocket.url=${POLYGON_WEBSOCKET_URL:wss://delayed.massive.com/v1/stocks}
polygon.symbols=${POLYGON_SYMBOLS:AAPL,GOOGL,MSFT,TSLA,AMZN}
```

## Switching Between Delayed and Real-time

### Delayed Data (Free)
- 15-minute delay
- No additional cost
- Good for development and testing

```env
POLYGON_WEBSOCKET_URL=wss://delayed.massive.com/v1/stocks
```

### Real-time Data (Paid)
- Live market data
- Requires subscription
- Production use

```env
POLYGON_WEBSOCKET_URL=wss://realtime.massive.com/v1/stocks
```

**No code changes required** - just update the environment variable!

## API Key Management

### Getting Your API Key

1. Sign up at https://massive.com
2. Navigate to Dashboard → API Keys
3. Create or copy your API key
4. Set in environment variable: `POLYGON_API_KEY`

### Security Best Practices

✅ **DO**:
- Store API key in environment variables
- Use `.env` file for local development
- Use cloud secret managers in production (AWS Secrets Manager, GCP Secret Manager, etc.)
- Rotate keys periodically

❌ **DON'T**:
- Hardcode API keys in source code
- Commit API keys to version control
- Share API keys publicly
- Use production keys in development

## Rate Limits & Performance

### Connection Limits
- **Default**: 1 concurrent WebSocket connection per asset class
- **Need more?** Contact Massive.com support

### Performance Tips
1. **Subscribe only to needed symbols** - Don't subscribe to all symbols
2. **Process messages quickly** - Slow consumers may be disconnected
3. **Use wired connection** - Avoid WiFi/VPN for lower latency
4. **Monitor buffer sizes** - Check for backpressure

### Handling Disconnections

Finbot automatically handles disconnections:

```java
@Override
public void onClose(int code, String reason, boolean remote) {
    log.warn("Connection closed: code={}, reason={}, remote={}", code, reason, remote);
    
    if (remote) {
        scheduleReconnection();  // Auto-reconnect after 5 seconds
    }
}
```

## Troubleshooting

### Issue: Authentication Failed

**Symptoms**: 
- "auth_failed" status message
- No data received

**Solutions**:
1. Verify API key is correct
2. Check API key is active in dashboard
3. Ensure no extra spaces in environment variable
4. Try regenerating API key

### Issue: No Data Received

**Symptoms**:
- Connected and authenticated
- No market data messages

**Solutions**:
1. Verify market is open (US markets: 9:30 AM - 4:00 PM ET)
2. Check symbols are valid US stocks
3. Ensure subscription message was sent
4. Check logs for subscription confirmation

### Issue: Connection Drops Frequently

**Symptoms**:
- Frequent reconnections
- "slow consumer" warnings

**Solutions**:
1. Reduce number of subscribed symbols
2. Optimize message processing speed
3. Check network stability
4. Increase processing resources

### Issue: Delayed Data Not Updating

**Symptoms**:
- Using delayed feed
- Data seems stale

**Solutions**:
1. Remember: 15-minute delay is expected
2. Check if market is currently open
3. Verify subscription is active
4. Check for error messages in logs

## Testing

### Test Connection Manually

Using `wscat`:

```bash
# Install wscat
npm install -g wscat

# Connect to delayed feed
wscat -c wss://delayed.massive.com/v1/stocks

# After connection, authenticate
{"action":"auth","key":"YOUR_API_KEY"}

# Subscribe to symbols
{"action":"subscribe","params":"AM.AAPL"}

# You should see data flowing
```

### Test in Finbot

```bash
# Start only ingestion service
docker compose up redis ingestion-service

# Check logs
docker compose logs -f ingestion-service

# Look for:
# - "Connected to Massive WebSocket API"
# - "Authentication successful"
# - "Subscribed to symbols: AAPL, GOOGL, ..."
# - "Processed aggregate for AAPL: close=..."
```

## Event Types Reference

| Event Type | Code | Description | Used by Finbot |
|------------|------|-------------|----------------|
| Trade | T | Individual trades | ❌ |
| Quote | Q | Bid/Ask quotes | ❌ |
| Aggregate (Second) | A | Second bars | ❌ |
| Aggregate (Minute) | AM | Minute bars | ✅ Yes |
| Aggregate (Hour) | AH | Hour bars | ❌ |
| Aggregate (Day) | AD | Daily bars | ❌ |

**Why Aggregate Minute (AM)?**
- Good balance between granularity and volume
- Reduces message frequency vs trades
- Sufficient for most analysis use cases
- Lower bandwidth requirements

## Migration from Polygon.io

If you were using the old Polygon.io API:

### Changes Required

1. **WebSocket URL**:
   - Old: `wss://socket.polygon.io/stocks`
   - New: `wss://delayed.massive.com/v1/stocks`

2. **Authentication**:
   - Old: `{"action":"auth","params":"API_KEY"}`
   - New: `{"action":"auth","key":"API_KEY"}`

3. **Field Names**: Mostly the same, but verify:
   - `sym` (not `s`) for symbol
   - Timestamps in Unix milliseconds

### Code Updates

Finbot has been updated to use the new Massive.com API format. No additional changes needed if you're using the latest version.

## Additional Resources

- **Massive.com Docs**: https://massive.com/docs
- **WebSocket Quickstart**: https://massive.com/docs/websocket/quickstart
- **REST API Quickstart**: https://massive.com/docs/rest/quickstart
- **Dashboard**: https://massive.com/dashboard
- **Support**: https://massive.com/contact

## Summary

✅ Finbot is fully integrated with Massive.com API
✅ Supports both delayed (free) and real-time (paid) feeds
✅ Auto-reconnection and error handling
✅ Normalized data model for downstream processing
✅ Easy configuration via environment variables
✅ Production-ready implementation

**Default Configuration**: Uses 15-minute delayed feed with the provided API key, ready to run out of the box!
