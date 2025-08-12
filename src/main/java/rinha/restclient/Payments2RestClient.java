package rinha.restclient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import rinha.dto.PaymentsRestClientRequest;

public interface Payments2RestClient {

    @POST
    @Path("/payments")
    Uni<Void> payments(PaymentsRestClientRequest request);
}
