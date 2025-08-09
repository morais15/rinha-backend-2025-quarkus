package rinha.worker;

import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
@RequiredArgsConstructor
public class RedisWorker {

    private final Redis redis;

    //    private final List<Request> batchRequests = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong localDefaultCount = new AtomicLong();
    private final AtomicLong localFallbackCount = new AtomicLong();

    @PostConstruct
    void start() {
        scheduler.scheduleWithFixedDelay(this::flushCountsToRedis, 0, 50, TimeUnit.MILLISECONDS);
    }

//    public void sendToRedis() {
//        List<Request> toSend;
//        synchronized (batchRequests) {
//            if (batchRequests.isEmpty()) {
//                return;
//            }
//            toSend = new ArrayList<>(batchRequests);
//            batchRequests.clear();
//        }
//
//        redis.batch(toSend).onComplete(ignore -> {
//        });
//    }
//
//    public void addToRedisQueue(Request request) {
//        batchRequests.add(request);
//    }

    public void incrementLocalDefaultCount() {
        localDefaultCount.incrementAndGet();
    }

    public void incrementLocalFallbackCount() {
        localFallbackCount.incrementAndGet();
    }

    public void flushCountsToRedis() {
        long toSendDefault = localDefaultCount.getAndSet(0);
        if (toSendDefault > 0) {
            redis.send(Request.cmd(Command.INCRBY).arg("payments:default:count").arg(Long.toString(toSendDefault)));
        }

        long toSendFallback = localFallbackCount.getAndSet(0);
        if (toSendFallback > 0) {
            redis.send(Request.cmd(Command.INCRBY).arg("payments:fallback:count").arg(Long.toString(toSendFallback)));
        }
    }
}
