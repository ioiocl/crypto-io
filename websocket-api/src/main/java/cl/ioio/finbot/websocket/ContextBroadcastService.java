package cl.ioio.finbot.websocket;

import cl.ioio.finbot.websocket.adapter.RedisMarketContextRepositoryImpl;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@Slf4j
public class ContextBroadcastService {

    private final RedisMarketContextRepositoryImpl contextRepository;
    private final MarketContextWebSocket contextWebSocket;
    private final List<String> symbols;

    @Inject
    public ContextBroadcastService(
            RedisMarketContextRepositoryImpl contextRepository,
            MarketContextWebSocket contextWebSocket,
            @ConfigProperty(name = "broadcast.symbols", defaultValue = "BTC,ETH,BNB,SOL,XRP") String symbolsConfig) {
        this.contextRepository = contextRepository;
        this.contextWebSocket = contextWebSocket;
        this.symbols = Arrays.stream(symbolsConfig.split(",")).map(String::trim).toList();
    }

    @Scheduled(every = "${broadcast.interval:1s}")
    void broadcastContexts() {
        for (String symbol : symbols) {
            if (MarketContextWebSocket.getConnectionCount(symbol) <= 0) {
                continue;
            }

            contextRepository.findLatestReactive(symbol)
                    .subscribe().with(
                            contextOpt -> contextOpt.ifPresent(context -> contextWebSocket.broadcastContext(symbol, context)),
                            error -> log.error("Error broadcasting context for {}", symbol, error)
                    );
        }
    }
}
