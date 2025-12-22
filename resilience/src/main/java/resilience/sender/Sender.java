package resilience.sender;

import resilience.model.ServerResponse;

import java.util.concurrent.CompletableFuture;

public interface Sender
{
    CompletableFuture<ServerResponse> send();
}
