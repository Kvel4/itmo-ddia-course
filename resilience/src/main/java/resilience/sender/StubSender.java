package resilience.sender;

import resilience.model.ServerResponse;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StubSender
        implements Sender
{
    private final Queue<ServerResponse> responses;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public StubSender(Queue<ServerResponse> responses)
    {
        this.responses = responses;
    }

    @Override
    public CompletableFuture<ServerResponse> send()
    {
        CompletableFuture<ServerResponse> future = new CompletableFuture<>();

        scheduler.schedule(() -> {
            ServerResponse resp = responses.poll();
            if (resp instanceof ServerResponse.Timeout) {
                // timeout
            }
            else {
                future.complete(resp);
            }
        }, 20, TimeUnit.MILLISECONDS);

        return future;
    }
}
