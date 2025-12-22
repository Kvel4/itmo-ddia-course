package resilience.strategy;

import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.Sender;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExponentialBackoffRetryStrategy
        implements SingleClientStrategy
{
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    @Override
    public CompletableFuture<Result> execute(
            StrategyConfig config,
            Sender sender
    )
    {
        CompletableFuture<Result> result = new CompletableFuture<>();
        result.completeOnTimeout(
                new Result.Failure(FailureReason.LATENCY_BUDGET_EXCEEDED),
                config.latencyBudget.toMillis(),
                TimeUnit.MILLISECONDS
        );

        attempt(sender, config, 0, result);

        return result;
    }

    private void attempt(
            Sender sender,
            StrategyConfig config,
            int backoffAttempt,
            CompletableFuture<Result> result
    )
    {
        CompletableFuture<ServerResponse> request = sender.send();
        request.completeOnTimeout(
                new ServerResponse.Timeout(),
                config.subrequestLatencyBudget.toMillis(),
                TimeUnit.MILLISECONDS
        ).whenComplete((response, _) -> {
            if (result.isDone()) {
                return;
            }

            boolean shouldBackoff = false;
            switch (response) {
                case ServerResponse.Ok ok -> result.complete(new Result.Success(ok.payload()));

                case ServerResponse.Timeout _ -> {
                    config.timeoutBudget--;
                    if (config.timeoutBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.TIMEOUT_BUDGET_EXHAUSTED));
                    }
                    else {
                        shouldBackoff = true;
                    }
                }

                case ServerResponse.ClientError _ -> {
                    config.fastErrorsBudget--;
                    if (config.fastErrorsBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.FAST_ERRORS_BUDGET_EXHAUSTED));
                    }
                    else {
                        attempt(sender, config, backoffAttempt, result);
                    }
                }

                case null, default -> {
                    config.failuresBudget--;
                    if (config.failuresBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.FAILURES_BUDGET_EXHAUSTED));
                    }
                    else {
                        shouldBackoff = true;
                    }
                }
            }

            if (shouldBackoff) {
                Duration delay = config.backoffBase.multipliedBy(1L << backoffAttempt);
                if (delay.compareTo(config.backoffCap) > 0) {
                    delay = config.backoffCap;
                }

                scheduler.schedule(
                        () -> attempt(sender, config, backoffAttempt + 1, result),
                        delay.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
        });
    }
}
