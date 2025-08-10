package rinha.service;

import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import rinha.dto.SummaryResponse;
import rinha.dto.TotalSummaryResponse;
import rinha.worker.RedisWorker;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@RequiredArgsConstructor
public class SaveService {

    private final RedisWorker redisWorker;
    private final Redis redis;

    private final AtomicReference<BigDecimal> firstAmount = new AtomicReference<>();

    public void saveDefault(BigDecimal amount) {
        setFirstAmount(amount);
        redisWorker.incrementLocalDefaultCount();
    }

    public void saveFallback(BigDecimal amount) {
        setFirstAmount(amount);
        redisWorker.incrementLocalFallbackCount();
    }

    private void setFirstAmount(BigDecimal amount) {
        firstAmount.compareAndSet(null, amount);
    }

    public Future<TotalSummaryResponse> totalSummary() {
        var req = Request.cmd(Command.MGET)
                .arg("payments:default:count")
                .arg("payments:fallback:count");

        return redis.send(req)
                .map(responses -> {
                    Response defaultCountResp = responses.get(0);
                    Response fallbackCountResp = responses.get(1);

                    int defaultCount = defaultCountResp == null || defaultCountResp.toString() == null
                            ? 0 : Integer.parseInt(defaultCountResp.toString());

                    int fallbackCount = fallbackCountResp == null || fallbackCountResp.toString() == null
                            ? 0 : Integer.parseInt(fallbackCountResp.toString());

                    BigDecimal defaultSum = firstAmount.get() != null
                            ? firstAmount.get().multiply(BigDecimal.valueOf(defaultCount))
                            : BigDecimal.ZERO;

                    BigDecimal fallbackSum = firstAmount.get() != null
                            ? firstAmount.get().multiply(BigDecimal.valueOf(fallbackCount))
                            : BigDecimal.ZERO;

                    SummaryResponse defaultResp = new SummaryResponse(defaultCount, defaultSum);
                    SummaryResponse fallbackResp = new SummaryResponse(fallbackCount, fallbackSum);

                    return new TotalSummaryResponse(defaultResp, fallbackResp);
                });
    }
}
