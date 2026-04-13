package cl.ioio.finbot.websocket;

import cl.ioio.finbot.domain.model.MarketContext;
import cl.ioio.finbot.websocket.adapter.RedisMarketContextRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/context/{symbol}")
@ApplicationScoped
@Slf4j
public class MarketContextWebSocket {

    private static final Map<String, Set<Session>> symbolSessions = new ConcurrentHashMap<>();

    @Inject
    RedisMarketContextRepositoryImpl contextRepository;

    @Inject
    ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session, @PathParam("symbol") String symbol) {
        symbolSessions.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet()).add(session);
        sendCurrentContext(session, symbol);
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
    }

    @OnError
    public void onError(Session session, @PathParam("symbol") String symbol, Throwable throwable) {
        log.error("Context websocket error for {}", symbol, throwable);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("symbol") String symbol) {
        if ("refresh".equalsIgnoreCase(message)) {
            sendCurrentContext(session, symbol);
        }
    }

    private void sendCurrentContext(Session session, String symbol) {
        contextRepository.findLatestReactive(symbol)
                .subscribe().with(
                        contextOpt -> {
                            try {
                                if (contextOpt.isPresent()) {
                                    String json = objectMapper.writeValueAsString(contextOpt.get());
                                    session.getAsyncRemote().sendText(json);
                                } else {
                                    session.getAsyncRemote().sendText("{\"error\":\"No context available\"}");
                                }
                            } catch (Exception e) {
                                log.error("Error sending context for {}", symbol, e);
                            }
                        },
                        error -> log.error("Error reading context for {}", symbol, error)
                );
    }

    public void broadcastContext(String symbol, MarketContext context) {
        Set<Session> sessions = symbolSessions.get(symbol);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(context);
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(json);
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting context for {}", symbol, e);
        }
    }

    public static int getConnectionCount(String symbol) {
        Set<Session> sessions = symbolSessions.get(symbol);
        return sessions != null ? sessions.size() : 0;
    }
}
