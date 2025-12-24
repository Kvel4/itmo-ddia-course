package resilience.strategy;

import resilience.model.Result;
import resilience.config.StrategyConfig;
import resilience.sender.Sender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MultiClientStrategy {
    CompletableFuture<Result> execute(
            StrategyConfig params,
            List<Sender> senders
    );
}
