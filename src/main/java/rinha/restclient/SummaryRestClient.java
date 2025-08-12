package rinha.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import rinha.dto.LocalSummaryResponse;

import java.time.Instant;

public interface SummaryRestClient {

    @GET
    @Path("/local-summary")
    LocalSummaryResponse localSummary(@QueryParam("from") Instant from, @QueryParam("to") Instant to);
}
