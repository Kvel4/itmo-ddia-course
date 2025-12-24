package resilience;

import org.junit.jupiter.api.Test;
import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.Sender;
import resilience.sender.StubSender;
import resilience.strategy.DefaultStrategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static resilience.model.FailureReason.FAILURES_BUDGET_EXHAUSTED;
import static resilience.model.FailureReason.FAST_ERRORS_BUDGET_EXHAUSTED;

class DefaultStrategyTest
{
    @Test
    void success()
            throws Exception
    {
        Sender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.Ok("ok"))));
        DefaultStrategy strategy = new DefaultStrategy();

        Result result = strategy.execute(TestUtils.noRetriesConfig(), sender).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void clientErrorFails()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.ClientError(400))));
        DefaultStrategy strategy = new DefaultStrategy();

        Result result = strategy.execute(TestUtils.noRetriesConfig(), sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FAST_ERRORS_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void serverErrorFails()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.ServerError(500))));
        DefaultStrategy strategy = new DefaultStrategy();

        Result result = strategy.execute(TestUtils.noRetriesConfig(), sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FAILURES_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void timeoutBudgetExceeded()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.Timeout())));
        StrategyConfig config = new StrategyConfig(
                0,
                0,
                0,
                Duration.ofSeconds(1),
                Duration.ofMillis(1),
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO
        );
        DefaultStrategy strategy = new DefaultStrategy();

        Result result = strategy.execute(config, sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.TIMEOUT_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void latencyBudgetExceeded()
            throws Exception
    {
        StubSender sender = new StubSender(new ArrayDeque<>(List.of(new ServerResponse.Ok("late"))));
        StrategyConfig config = new StrategyConfig(
                0,
                0,
                0,
                Duration.ofMillis(1),
                Duration.ofMillis(100),
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO
        );
        DefaultStrategy strategy = new DefaultStrategy();

        Result result = strategy.execute(config, sender).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.LATENCY_BUDGET_EXCEEDED,
                ((Result.Failure) result).reason()
        );
    }
}
