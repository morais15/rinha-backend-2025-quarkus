package rinha.worker;

import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import rinha.dto.PaymentsRestClientRequest;
import rinha.restclient.Payments1RestClient;
import rinha.restclient.Payments2RestClient;
import rinha.service.SaveService;

import java.util.concurrent.*;

@ApplicationScoped
public class PaymentWorker {

    private final Payments1RestClient payments1RestClient;
    private final Payments2RestClient payments2RestClient;
    private final SaveService saveService;
    private final ExecutorService executorService;

    private final ConcurrentLinkedQueue<PaymentsRestClientRequest> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PaymentWorker(Payments1RestClient payments1RestClient,
                         Payments2RestClient payments2RestClient,
                         SaveService saveService,
                         @VirtualThreads ExecutorService executorService) {
        this.payments1RestClient = payments1RestClient;
        this.payments2RestClient = payments2RestClient;
        this.saveService = saveService;
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
            payments1RestClient.payments(request);
            saveService.saveDefault(request.amount);
        } catch (Exception e) {
            payments2RestClient.payments(request);
            saveService.saveFallback(request.amount);
        }
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
