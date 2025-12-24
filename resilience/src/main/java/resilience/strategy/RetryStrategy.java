package resilience.strategy;

import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.Sender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RetryStrategy
        implements SingleClientStrategy
{
    @Override
    public CompletableFuture<Result> execute(StrategyConfig config, Sender sender)
    {
        CompletableFuture<Result> result = new CompletableFuture<>();
        result.completeOnTimeout(
                new Result.Failure(FailureReason.LATENCY_BUDGET_EXCEEDED),
                config.latencyBudget.toMillis(),
                TimeUnit.MILLISECONDS
        );

        attempt(sender, config, result);

        return result;
    }

    protected void attempt(Sender sender, StrategyConfig config, CompletableFuture<Result> result)
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

            switch (response) {
                case ServerResponse.Ok ok -> result.complete(new Result.Success(ok.payload()));

                case ServerResponse.Timeout _ -> {
                    config.timeoutBudget--;
                    if (config.timeoutBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.TIMEOUT_BUDGET_EXHAUSTED));
                    }
                    else {
                        attempt(sender, config, result);
                    }
                }

                case ServerResponse.ClientError _ -> {
                    config.fastErrorsBudget--;
                    if (config.fastErrorsBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.FAST_ERRORS_BUDGET_EXHAUSTED));
                    }
                    else {
                        attempt(sender, config, result);
                    }
                }

                case null, default -> {
                    config.failuresBudget--;
                    if (config.failuresBudget < 0) {
                        result.complete(new Result.Failure(FailureReason.FAILURES_BUDGET_EXHAUSTED));
                    }
                    else {
                        attempt(sender, config, result);
                    }
                }
            }
        });
    }
}
