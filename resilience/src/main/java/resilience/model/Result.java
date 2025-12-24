package resilience.model;

public sealed interface Result
{
    record Success(String payload)
            implements Result {}

    record Failure(FailureReason reason)
            implements Result {}
}
