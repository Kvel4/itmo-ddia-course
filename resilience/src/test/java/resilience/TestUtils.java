package resilience;

import resilience.config.StrategyConfig;

import java.time.Duration;

public class TestUtils {

    public static StrategyConfig defaultConfig() {
        return new StrategyConfig(
                1,
                1,
                1,
                Duration.ofSeconds(2),
                Duration.ofMillis(100),
                Duration.ofMillis(50),
                Duration.ofMillis(200)
        );
    }

    public static StrategyConfig noRetriesConfig() {
        return new StrategyConfig(
                0,
                0,
                0,
                Duration.ofSeconds(1),
                Duration.ofMillis(100),
                Duration.ZERO,
                Duration.ZERO
        );
    }
}
