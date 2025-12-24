package resilience.strategy;

import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.Sender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DefaultStrategy
        implements SingleClientStrategy
{
    @Override
    public CompletableFuture<Result> execute(StrategyConfig config, Sender sender)
    {
        CompletableFuture<Result> result = new CompletableFuture<>();
        CompletableFuture<ServerResponse> request = sender.send();

        result.completeOnTimeout(
                new Result.Failure(FailureReason.LATENCY_BUDGET_EXCEEDED),
                config.latencyBudget.toMillis(),
                TimeUnit.MILLISECONDS
        ).whenComplete((_, _) -> {
            if (!request.isDone()) {
                request.cancel(true);
            }
        });

        request.completeOnTimeout(
                new ServerResponse.Timeout(),
                config.subrequestLatencyBudget.toMillis(),
                TimeUnit.MILLISECONDS
        ).whenComplete((response, _) -> {
            if (result.isDone()) {
                return;
            }

            switch (response) {
                case ServerResponse.Ok(String payload) -> result.complete(new Result.Success(payload));
                case ServerResponse.Timeout _ -> result.complete(new Result.Failure(FailureReason.TIMEOUT_BUDGET_EXHAUSTED));
                case ServerResponse.ClientError _ -> result.complete(new Result.Failure(FailureReason.FAST_ERRORS_BUDGET_EXHAUSTED));
                case null, default -> result.complete(new Result.Failure(FailureReason.FAILURES_BUDGET_EXHAUSTED));
            }
        });

        return result;
    }
}
