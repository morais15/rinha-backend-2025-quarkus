package rinha.restclient;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import rinha.dto.PaymentsRestClientRequest;

public interface Payments1RestClient {

    @POST
    @Path("/payments")
    void payments(PaymentsRestClientRequest request);
}
