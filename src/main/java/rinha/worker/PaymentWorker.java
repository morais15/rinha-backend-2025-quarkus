package rinha.worker;

import io.quarkus.runtime.Startup;
import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import rinha.dto.PaymentsRestClientRequest;
import rinha.restclient.Payments1RestClient;
import rinha.service.DatabaseService;

import java.time.Duration;
import java.util.concurrent.*;

@ApplicationScoped
@Startup
public class PaymentWorker {

    private final Payments1RestClient payments1RestClient;
    private final DatabaseService databaseService;
    private final ExecutorService executorService;

    private final ConcurrentLinkedQueue<PaymentsRestClientRequest> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Semaphore semaphore = new Semaphore(50);

    public PaymentWorker(Payments1RestClient payments1RestClient,
                         DatabaseService databaseService,
                         @VirtualThreads ExecutorService executorService) {
        this.payments1RestClient = payments1RestClient;
        this.databaseService = databaseService;
        this.executorService = executorService;
    }

    @PostConstruct
    void startSchedule() {
        scheduler.scheduleWithFixedDelay(this::runSchedule, 0, 50, TimeUnit.MILLISECONDS);
    }

    public void saveInQueue(PaymentsRestClientRequest request) {
        queue.add(request);
    }

    public void sendPayment(PaymentsRestClientRequest request) {
        try {
            semaphore.acquire();

            payments1RestClient.payments(request)
                    .onItem().invoke(() -> databaseService.saveDefault(request))
                    .onFailure().retry().withBackOff(Duration.ofMillis(50), Duration.ofMillis(200)).indefinitely()
                    .onTermination().invoke(semaphore::release)
                    .subscribe().with(ignored -> {
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void runSchedule() {
        processQueues(queue);
    }

    public void processQueues(ConcurrentLinkedQueue<PaymentsRestClientRequest> queueToProcess) {
        PaymentsRestClientRequest payment;
        while ((payment = queueToProcess.poll()) != null) {
            final PaymentsRestClientRequest p = payment;
            executorService.submit(() -> sendPayment(p));
        }
    }
}
