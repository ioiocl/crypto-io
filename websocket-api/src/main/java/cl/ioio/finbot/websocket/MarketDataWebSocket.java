package cl.ioio.finbot.websocket;

import cl.ioio.finbot.domain.model.MarketSnapshot;
import cl.ioio.finbot.websocket.adapter.RedisSnapshotRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server endpoint for real-time market data streaming
 * Driver adapter - exposes market snapshots to clients
 */
@ServerEndpoint("/ws/market/{symbol}")
@ApplicationScoped
@Slf4j
public class MarketDataWebSocket {
    
    // Map of symbol -> Set of sessions subscribed to that symbol
    private static final Map<String, Set<Session>> symbolSessions = new ConcurrentHashMap<>();
    
    @Inject
    RedisSnapshotRepositoryImpl snapshotRepository;
    
    @Inject
    ObjectMapper objectMapper;
    
    @OnOpen
    public void onOpen(Session session, @PathParam("symbol") String symbol) {
        symbolSessions.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("Client connected: sessionId={}, symbol={}", session.getId(), symbol);
        
        // Send current snapshot immediately
        sendCurrentSnapshot(session, symbol);
    }
    
    @OnClose
    public void onClose(Session session, @PathParam("symbol") String symbol) {
        Set<Session> sessions = symbolSessions.get(symbol);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                symbolSessions.remove(symbol);
            }
        }
        log.info("Client disconnected: sessionId={}, symbol={}", session.getId(), symbol);
    }
    
    @OnError
    public void onError(Session session, @PathParam("symbol") String symbol, Throwable throwable) {
        log.error("WebSocket error for session {} on symbol {}", session.getId(), symbol, throwable);
    }
    
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("symbol") String symbol) {
        log.debug("Received message from client: sessionId={}, symbol={}, message={}", 
                session.getId(), symbol, message);
        
        // Handle client requests (e.g., refresh, subscribe to additional symbols)
        if ("refresh".equalsIgnoreCase(message)) {
            sendCurrentSnapshot(session, symbol);
        }
    }
    
    /**
     * Send current snapshot to a specific session (non-blocking)
     */
    private void sendCurrentSnapshot(Session session, String symbol) {
        snapshotRepository.findLatestReactive(symbol)
            .subscribe().with(
                snapshotOpt -> {
                    try {
                        if (snapshotOpt.isPresent()) {
                            String json = objectMapper.writeValueAsString(snapshotOpt.get());
                            session.getAsyncRemote().sendText(json);
                            log.debug("Sent snapshot to session {}: {}", session.getId(), symbol);
                        } else {
                            String errorMsg = String.format("{\"error\":\"No data available for %s\"}", symbol);
                            session.getAsyncRemote().sendText(errorMsg);
                        }
                    } catch (Exception e) {
                        log.error("Error sending snapshot to session {}", session.getId(), e);
                    }
                },
                error -> log.error("Error retrieving snapshot for session {}", session.getId(), error)
            );
    }
    
    /**
     * Broadcast snapshot to all sessions subscribed to a symbol
     */
    public void broadcastSnapshot(String symbol, MarketSnapshot snapshot) {
        Set<Session> sessions = symbolSessions.get(symbol);
        
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active sessions for symbol: {}", symbol);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(json);
                }
            }
            
            log.debug("Broadcasted snapshot for {} to {} sessions", symbol, sessions.size());
            
        } catch (Exception e) {
            log.error("Error broadcasting snapshot for " + symbol, e);
        }
    }
    
    /**
     * Get all active symbols with connected clients
     */
    public static Set<String> getActiveSymbols() {
        return symbolSessions.keySet();
    }
    
    /**
     * Get number of active connections for a symbol
     */
    public static int getConnectionCount(String symbol) {
        Set<Session> sessions = symbolSessions.get(symbol);
        return sessions != null ? sessions.size() : 0;
    }
}
