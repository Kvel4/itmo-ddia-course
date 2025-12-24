package resilience.model;

public enum FailureReason
{
    FAST_ERRORS_BUDGET_EXHAUSTED,
    FAILURES_BUDGET_EXHAUSTED,
    TIMEOUT_BUDGET_EXHAUSTED,
    LATENCY_BUDGET_EXCEEDED
}
