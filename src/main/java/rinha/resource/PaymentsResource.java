package rinha.resource;

import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import rinha.dto.PaymentsRequest;
import rinha.dto.PaymentsRestClientRequest;
import rinha.dto.TotalSummaryResponse;
import rinha.service.SaveService;
import rinha.worker.PaymentWorker;

import java.time.Instant;

@Path("")
@RequiredArgsConstructor
public class PaymentsResource {

    private final PaymentWorker paymentWorker;
    private final SaveService saveService;

    private static final Response acceptedResponse = Response.accepted().build();

    @POST
    @Path("/payments")
    @NonBlocking
    public Response payments(PaymentsRequest request) {

        var saveRequest = new PaymentsRestClientRequest(request.correlationId, request.amount, Instant.now());

        paymentWorker.saveInQueue(saveRequest);

        return acceptedResponse;
    }

//    @POST
//    @Path("/payments")
//    public void payments(PaymentsRequest request, @Suspended AsyncResponse asyncResponse) {
//        var saveRequest = new PaymentsRestClientRequest(
//                request.correlationId,
//                request.amount,
//                Instant.now()
//        );
//
//        // Enfileira rápido e responde imediatamente
//        sendPaymentService.saveInQueue(saveRequest);
//
//        // Retorna 202 Accepted imediatamente
//        asyncResponse.resume(Response.accepted().build());
//    }

    @GET
    @Path("/payments-summary")
    public TotalSummaryResponse summary() {
        return saveService.totalSummary().toCompletionStage().toCompletableFuture().join();
    }
}
