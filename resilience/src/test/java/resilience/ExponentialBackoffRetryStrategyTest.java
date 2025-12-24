package resilience;

import org.junit.jupiter.api.Test;
import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.StubSender;
import resilience.strategy.ExponentialBackoffRetryStrategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ExponentialBackoffRetryStrategyTest
        extends RetryStrategyTest
{
    @Test
    void succeedsAfterMultipleServerErrors()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500),
                new ServerResponse.ServerError(500),
                new ServerResponse.Ok("ok")
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                2,
                0,
                Duration.ofSeconds(2),
                Duration.ofMillis(100),
                Duration.ofMillis(50),
                Duration.ofMillis(200),
                Duration.ZERO
        );
        ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy();

        Result result = strategy.execute(config, sender).get();
        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void backoffNotAppliedForClientError()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ClientError(400),
                new ServerResponse.ClientError(400),
                new ServerResponse.ClientError(400),
                new ServerResponse.Ok("ok")
        )));
        StrategyConfig config = new StrategyConfig(
                3,
                0,
                0,
                Duration.ofMillis(150),
                Duration.ofMillis(50),
                Duration.ofMillis(30),
                Duration.ofMillis(200),
                Duration.ZERO
        );
        ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy();

        Result result = strategy.execute(config, sender).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void latencyBudgetExceededDueToBackoff()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500),
                new ServerResponse.ServerError(500),
                new ServerResponse.ServerError(500)
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                3,
                0,
                Duration.ofMillis(150),
                Duration.ofMillis(50),
                Duration.ofMillis(30),
                Duration.ofMillis(200),
                Duration.ZERO
        );
        ExponentialBackoffRetryStrategy strategy = new ExponentialBackoffRetryStrategy();

        Result result = strategy.execute(config, sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.LATENCY_BUDGET_EXCEEDED,
                ((Result.Failure) result).reason()
        );
    }
}
