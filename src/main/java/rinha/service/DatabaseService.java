package rinha.service;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import rinha.dto.LocalSummaryResponse;
import rinha.dto.PaymentsRestClientRequest;
import rinha.dto.SummaryResponse;
import rinha.dto.TotalSummaryResponse;
import rinha.restclient.SummaryRestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@RequiredArgsConstructor
@Startup
public class DatabaseService {

    private final SummaryRestClient summaryRestClient;

    private final AtomicReference<BigDecimal> firstAmount = new AtomicReference<>();
    private final ConcurrentLinkedQueue<Instant> defaultReqs = new ConcurrentLinkedQueue<>();

    public void saveDefault(PaymentsRestClientRequest request) {
        setFirstAmount(request.amount);
        defaultReqs.add(request.requestedAt);
    }

    private void setFirstAmount(BigDecimal amount) {
        firstAmount.compareAndSet(null, amount);
    }

    public LocalSummaryResponse localSummary(Instant from, Instant to) {
        if (from == null || to == null) {
            return new LocalSummaryResponse((long) defaultReqs.size(), 0L);
        }

        long defaultCount = 0;
        for (Instant i : defaultReqs) {
            if (!i.isBefore(from) && !i.isAfter(to)) {
                defaultCount++;
            }
        }

        return new LocalSummaryResponse(defaultCount, 0L);
    }

    public TotalSummaryResponse totalSummary(Instant from, Instant to) {
        var otherApiSummary = summaryRestClient.localSummary(from, to);
        var localSummary = localSummary(from, to);

        var amount = firstAmount.get();
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        var totalDefaultReqs = otherApiSummary.defaultReqs + localSummary.defaultReqs;

        var defaultSummary = new SummaryResponse(totalDefaultReqs, amount.multiply(BigDecimal.valueOf(totalDefaultReqs)));
        var fallbackSummary = new SummaryResponse(0L, BigDecimal.ZERO);

        return new TotalSummaryResponse(defaultSummary, fallbackSummary);
    }
}
