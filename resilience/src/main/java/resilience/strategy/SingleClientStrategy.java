package resilience.strategy;

import resilience.model.Result;
import resilience.config.StrategyConfig;
import resilience.sender.Sender;

import java.util.concurrent.CompletableFuture;

public interface SingleClientStrategy {
    CompletableFuture<Result> execute(StrategyConfig params, Sender sender);
}
