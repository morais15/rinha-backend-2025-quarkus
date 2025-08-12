package rinha.resource;

import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import rinha.dto.LocalSummaryResponse;
import rinha.dto.PaymentsRequest;
import rinha.dto.PaymentsRestClientRequest;
import rinha.dto.TotalSummaryResponse;
import rinha.service.DatabaseService;
import rinha.worker.PaymentWorker;

import java.time.Instant;

@Path("")
@RequiredArgsConstructor
public class PaymentsResource {

    private final PaymentWorker paymentWorker;
    private final DatabaseService databaseService;

    private static final Response acceptedResponse = Response.accepted().build();

    @POST
    @Path("/payments")
    @NonBlocking
    public Response payments(PaymentsRequest request) {

        final var saveRequest = new PaymentsRestClientRequest(request.correlationId, request.amount, Instant.now());

        paymentWorker.saveInQueue(saveRequest);

        return acceptedResponse;
    }

    @GET
    @Path("/payments-summary")
    public TotalSummaryResponse summary(@QueryParam("from") Instant from, @QueryParam("to") Instant to) {
        return databaseService.totalSummary(from, to);
    }

    @GET
    @Path("/local-summary")
    public LocalSummaryResponse localSummary(@QueryParam("from") Instant from, @QueryParam("to") Instant to) {
//        System.out.println("from: " + from);
//        System.out.println("to: " + to);

        return databaseService.localSummary(from, to);
    }
}
