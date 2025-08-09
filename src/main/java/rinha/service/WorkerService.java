package rinha.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import rinha.dto.PaymentsRestClientRequest;
import rinha.restclient.Payments1RestClient;
import rinha.restclient.Payments2RestClient;

import java.util.concurrent.*;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkerService {

    private final Payments1RestClient payments1RestClient;
    private final Payments2RestClient payments2RestClient;
    private final SaveService saveService;
    private final ExecutorService executorService;

    private final ConcurrentLinkedQueue<PaymentsRestClientRequest> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    void startSchedule() {
        scheduler.scheduleWithFixedDelay(this::runSchedule, 0, 1000, TimeUnit.MILLISECONDS);
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
//
//
//        return payments1RestClient.payments(request)
//                .onItem().invoke(() -> saveService.saveDefault(request.amount))
//                .onFailure().recoverWithUni(() ->
//                        payments2RestClient.payments(request)
//                                .onItem().invoke(() -> saveService.saveFallback(request.amount))
//                );
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
