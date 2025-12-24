package resilience;

import org.junit.jupiter.api.Test;
import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.StubSender;
import resilience.strategy.RoundRobinMultiClientStrategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RoundRobinMultiClientStrategyTest
{
    @Test
    void cyclesThroughClients()
            throws Exception
    {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s3 = new StubSender(new ArrayDeque<>(List.of(
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
        RoundRobinMultiClientStrategy rr = new RoundRobinMultiClientStrategy();

        Result result = rr.execute(config, List.of(s1, s2, s3)).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void allClientsFailAndFailureBudgetExceeded() throws Exception {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s3 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
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
        RoundRobinMultiClientStrategy rr = new RoundRobinMultiClientStrategy();

        Result result = rr.execute(config, List.of(s1, s2, s3)).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.FAILURES_BUDGET_EXHAUSTED,
                ((Result.Failure) result).reason()
        );
    }

    @Test
    void timeoutOnFirstClientThenSuccessOnNext() throws Exception {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Ok("ok")
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                0,
                1,
                Duration.ofSeconds(2),
                Duration.ofMillis(50),
                Duration.ofMillis(50),
                Duration.ofMillis(200),
                Duration.ZERO
        );
        RoundRobinMultiClientStrategy rr = new RoundRobinMultiClientStrategy();

        Result result = rr.execute(config, List.of(s1, s2)).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void latencyBudgetExceededAcrossMultipleClients() throws Exception {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                2,
                0,
                Duration.ofMillis(30),
                Duration.ofMillis(50),
                Duration.ofMillis(50),
                Duration.ofMillis(200),
                Duration.ZERO
        );
        RoundRobinMultiClientStrategy rr = new RoundRobinMultiClientStrategy();

        Result result = rr.execute(config, List.of(s1, s2)).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(
                FailureReason.LATENCY_BUDGET_EXCEEDED,
                ((Result.Failure) result).reason()
        );
    }
}
