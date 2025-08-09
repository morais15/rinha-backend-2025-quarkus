package rinha.service;

import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import rinha.dto.SummaryResponse;
import rinha.dto.TotalSummaryResponse;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@RequiredArgsConstructor
public class SaveService {

    private final Redis redis;

    private static final String DEFAULT_TYPE = "default";
    private static final String FALLBACK_TYPE = "fallback";

    private final AtomicReference<BigDecimal> firstAmount = new AtomicReference<>();
    private final AtomicInteger reqs = new AtomicInteger();

    public void saveDefault(BigDecimal amount) {
        save(DEFAULT_TYPE, amount);
        reqs.incrementAndGet();
    }

    public void saveFallback(BigDecimal amount) {
        save(FALLBACK_TYPE, amount);
    }

    private void save(String type, BigDecimal amount) {
        firstAmount.compareAndSet(null, amount);

        var req = Request.cmd(Command.INCR).arg("payments:" + type + ":count");
        redis.send(req).onComplete(ignore -> {
        });
    }

    private Future<SummaryResponse> defaultSummary() {
        return summary(DEFAULT_TYPE);
    }

    private Future<SummaryResponse> fallbackSummary() {
        return summary(FALLBACK_TYPE);
    }

    private Future<SummaryResponse> summary(String type) {
        var reqCount = Request.cmd(Command.GET).arg("payments:" + type + ":count");

        return redis.send(reqCount)
                .map(countResp -> {
                    int count = (countResp == null || countResp.toString() == null)
                            ? 0
                            : Integer.parseInt(countResp.toString());

                    BigDecimal sum = firstAmount.get() != null
                            ? firstAmount.get().multiply(BigDecimal.valueOf(count))
                            : BigDecimal.ZERO;

                    return new SummaryResponse(count, sum);
                });
    }

    public Future<TotalSummaryResponse> totalSummary() {
        Future<SummaryResponse> defaultFuture = defaultSummary();
        Future<SummaryResponse> fallbackFuture = fallbackSummary();

//        System.out.println("reqs: " + reqs.get());

        return Future.all(defaultFuture, fallbackFuture)
                .map(cf -> {
                    SummaryResponse defaultResp = defaultFuture.result();
                    SummaryResponse fallbackResp = fallbackFuture.result();
                    return new TotalSummaryResponse(defaultResp, fallbackResp);
                });
    }
}
