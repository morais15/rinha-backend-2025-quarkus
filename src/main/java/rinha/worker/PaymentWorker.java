package rinha.worker;

import io.quarkus.runtime.Startup;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import rinha.dto.PaymentsRestClientRequest;
import rinha.restclient.Payments1RestClient;
import rinha.restclient.Payments2RestClient;
import rinha.service.DatabaseService;

import java.util.concurrent.*;

@ApplicationScoped
@Startup
public class PaymentWorker {

    private final Payments1RestClient payments1RestClient;
    private final Payments2RestClient payments2RestClient;
    private final DatabaseService databaseService;
    private final ExecutorService executorService;

    private final ConcurrentLinkedQueue<PaymentsRestClientRequest> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PaymentWorker(Payments1RestClient payments1RestClient,
                         Payments2RestClient payments2RestClient,
                         DatabaseService databaseService,
                         @VirtualThreads ExecutorService executorService) {
        this.payments1RestClient = payments1RestClient;
        this.payments2RestClient = payments2RestClient;
        this.databaseService = databaseService;
        this.executorService = executorService;
    }

    @PostConstruct
    void startSchedule() {
        scheduler.scheduleWithFixedDelay(this::runSchedule, 0, 200, TimeUnit.MILLISECONDS);
    }

    public void saveInQueue(PaymentsRestClientRequest request) {
        queue.add(request);
    }

    public void sendPayment(PaymentsRestClientRequest request) {
        payments1RestClient.payments(request)
                .onItem().invoke(() -> databaseService.saveDefault(request))
                .onFailure().recoverWithUni(() ->
                        payments2RestClient.payments(request)
                                .onItem().invoke(() -> databaseService.saveFallback(request))
                                .onFailure().invoke(() -> saveInQueue(request))
                                .onFailure().recoverWithUni(() -> Uni.createFrom().voidItem())
                )
                .subscribe().with(ignored -> {
                });
    }

    public void runSchedule() {
//        System.out.println("size: " + queue.size());

        PaymentsRestClientRequest payment;
        while ((payment = queue.poll()) != null) {
            PaymentsRestClientRequest p = payment;
            executorService.submit(() -> sendPayment(p));
        }
    }
}
