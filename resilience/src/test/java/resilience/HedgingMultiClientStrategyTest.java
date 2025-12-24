package resilience;

import org.junit.jupiter.api.Test;
import resilience.config.StrategyConfig;
import resilience.model.FailureReason;
import resilience.model.Result;
import resilience.model.ServerResponse;
import resilience.sender.StubSender;
import resilience.strategy.HedgingMultiClientStrategy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HedgingMultiClientStrategyTest
{
    @Test
    void firstClientSucceedsImmediately()
            throws Exception
    {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Ok("ok")
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Ok("late")
        )));
        HedgingMultiClientStrategy strategy = new HedgingMultiClientStrategy();

        Result result = strategy.execute(TestUtils.defaultConfig(), List.of(s1, s2)).get();

        assertInstanceOf(Result.Success.class, result);
        assertEquals("ok", ((Result.Success) result).payload());
    }

    @Test
    void hedgeAndSucceeds()
            throws Exception
    {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Ok("ok")
        )));
        HedgingMultiClientStrategy strategy = new HedgingMultiClientStrategy();

        Result result = strategy.execute(TestUtils.defaultConfig(), List.of(s1, s2)).get();

        assertInstanceOf(Result.Success.class, result);
    }

    @Test
    void latencyBudgetExceeded()
            throws Exception
    {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StrategyConfig config = new StrategyConfig(
                0,
                0,
                0,
                Duration.ofMillis(100),
                Duration.ofMillis(50),
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(50)
        );
        HedgingMultiClientStrategy strategy = new HedgingMultiClientStrategy();

        Result result = strategy.execute(config, List.of(s1, s2)).get();

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(FailureReason.LATENCY_BUDGET_EXCEEDED, ((Result.Failure) result).reason());
    }

    @Test
    void lastClientWinsHedge()
            throws Exception
    {
        StubSender s1 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s2 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.ServerError(500)
        )));
        StubSender s3 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s4 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Timeout()
        )));
        StubSender s5 = new StubSender(new ArrayDeque<>(List.of(
                new ServerResponse.Ok("win")
        )));
        HedgingMultiClientStrategy strategy = new HedgingMultiClientStrategy();

        Result result = strategy.execute(TestUtils.defaultConfig(), List.of(s1, s2, s3, s4, s5)).get();

        assertInstanceOf(Result.Success.class, result);
        assertEquals("win", ((Result.Success) result).payload());
    }
}
