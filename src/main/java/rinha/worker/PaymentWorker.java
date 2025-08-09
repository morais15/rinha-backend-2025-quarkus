package rinha.worker;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import rinha.dto.PaymentsRestClientRequest;
import rinha.restclient.Payments1RestClient;
import rinha.restclient.Payments2RestClient;
import rinha.service.SaveService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@ApplicationScoped
@RequiredArgsConstructor
public class PaymentWorker {

    private final Payments1RestClient payments1RestClient;
    private final Payments2RestClient payments2RestClient;
    private final SaveService saveService;

    private final LinkedBlockingQueue<PaymentsRestClientRequest> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(6);

    @PostConstruct
    void start() {
        int workers = 6;
        for (int i = 0; i < workers; i++) {
            executorService.submit(this::processQueue);
        }
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


//        return payments1RestClient.payments(request)
//                .onItem().invoke(() -> saveService.saveDefault(request.amount))
//                .onFailure().recoverWithUni(() ->
//                        payments2RestClient.payments(request)
//                                .onItem().invoke(() -> saveService.saveFallback(request.amount))
//                );
    }

    private void processQueue() {
        while (true) {
            try {
                PaymentsRestClientRequest request = queue.take();
                sendPayment(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
