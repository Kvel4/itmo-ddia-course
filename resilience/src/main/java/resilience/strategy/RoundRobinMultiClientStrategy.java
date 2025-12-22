package resilience.strategy;

import resilience.config.StrategyConfig;
import resilience.model.Result;
import resilience.sender.Sender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinMultiClientStrategy
        implements MultiClientStrategy
{
    private int index = 0;
    private final RetryStrategy delegate = new RetryStrategy();

    @Override
    public CompletableFuture<Result> execute(StrategyConfig params, List<Sender> senders)
    {
        Sender rrSender = () -> {
            int i = Math.abs(index++ % senders.size());
            return senders.get(i).send();
        };

        return delegate.execute(params, rrSender);
    }
}
