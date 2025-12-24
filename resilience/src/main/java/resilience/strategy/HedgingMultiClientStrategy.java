package resilience.strategy;

import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.sender.Sender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HedgingMultiClientStrategy
        implements MultiClientStrategy
{
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final SingleClientStrategy singleClientStrategy = new DefaultStrategy();

    @Override
    public CompletableFuture<Result> execute(StrategyConfig config, List<Sender> senders)
    {
        CompletableFuture<Result> result = new CompletableFuture<>();

        result.completeOnTimeout(new Result.Failure(FailureReason.LATENCY_BUDGET_EXCEEDED), config.latencyBudget.toMillis(), TimeUnit.MILLISECONDS);

        if (senders.isEmpty()) {
            result.complete(new Result.Failure(FailureReason.FAILURES_BUDGET_EXHAUSTED));
            return result;
        }

        executeSingle(config, senders.getFirst(), result);

        scheduler.schedule(() -> {
            if (result.isDone()) {
                return;
            }

            for (int i = 1; i < senders.size(); i++) {
                executeSingle(config, senders.get(i), result);
            }
        }, config.hedgingDelay.toMillis(), TimeUnit.MILLISECONDS);

        return result;
    }

    private void executeSingle(StrategyConfig config, Sender sender, CompletableFuture<Result> result)
    {
        if (result.isDone()) {
            return;
        }

        singleClientStrategy.execute(config, sender).whenComplete((r, _) -> {
            if (result.isDone()) {
                return;
            }

            if (r instanceof Result.Success success) {
                result.complete(success);
            }
        });
    }
}