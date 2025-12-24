package resilience.config;

import java.time.Duration;

public class StrategyConfig
{
    public int fastErrorsBudget;
    public int failuresBudget;
    public int timeoutBudget;

    public final Duration latencyBudget;
    public final Duration subrequestLatencyBudget;

    public final Duration backoffBase;
    public final Duration backoffCap;

    public final Duration hedgingDelay;

    public StrategyConfig(
            int fastErrorsBudget,
            int failuresBudget,
            int timeoutBudget,
            Duration latencyBudget,
            Duration subrequestLatencyBudget,
            Duration backoffBase,
            Duration backoffCap,
            Duration hedgingDelay
    )
    {
        this.fastErrorsBudget = fastErrorsBudget;
        this.failuresBudget = failuresBudget;
        this.timeoutBudget = timeoutBudget;
        this.latencyBudget = latencyBudget;
        this.subrequestLatencyBudget = subrequestLatencyBudget;
        this.backoffBase = backoffBase;
        this.backoffCap = backoffCap;
        this.hedgingDelay = hedgingDelay;
    }
}
