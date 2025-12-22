package resilience;

import org.junit.jupiter.api.Test;
import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.Sender;
import resilience.sender.StubSender;
import resilience.strategy.RetryStrategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RetryStrategyTest
{
    @Test
    void successOnFirstAttempt()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.Ok("ok"))));
        RetryStrategy strategy = new RetryStrategy();
        Result result = strategy.execute(TestUtils.defaultConfig(), sender).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void retryAfterServerErrorThenSuccess()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500),
                new ServerResponse.Ok("ok")
        )));
        RetryStrategy strategy = new RetryStrategy();
        Result result = strategy.execute(TestUtils.defaultConfig(), sender).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void fastErrorsBudgetExceeded()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ClientError(400),
                new ServerResponse.ClientError(400)
        )));
        RetryStrategy strategy = new RetryStrategy();
        Result result = strategy.execute(TestUtils.defaultConfig(), sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.FAST_ERRORS_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void failuresBudgetExceeded()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500),
                new ServerResponse.ServerError(500)
        )));
        RetryStrategy strategy = new RetryStrategy();
        Result result = strategy.execute(TestUtils.defaultConfig(), sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.FAILURES_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void timeoutBudgetExceeded()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout(),
                new ServerResponse.Timeout()
        )));
        RetryStrategy strategy = new RetryStrategy();
        Result result = strategy.execute(TestUtils.defaultConfig(), sender).get();

        assertEquals(
                FailureReason.TIMEOUT_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void latencyBudgetExceeded()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout(),
                new ServerResponse.Ok("late")
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                0,
                1,
                Duration.ofMillis(30),
                Duration.ofMillis(20),
                Duration.ZERO,
                Duration.ZERO
        );
        RetryStrategy strategy = new RetryStrategy();

        Result result = strategy.execute(config, sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.LATENCY_BUDGET_EXCEEDED,
                ((Result.Failure) result).reason()
        );
    }
}
