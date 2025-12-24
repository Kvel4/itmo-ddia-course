package resilience.model;

public sealed interface ServerResponse
{
    record Ok(String payload)
            implements ServerResponse {}

    record ClientError(int code)
            implements ServerResponse {}

    record ServerError(int code)
            implements ServerResponse {}

    record Timeout()
            implements ServerResponse {}
}
