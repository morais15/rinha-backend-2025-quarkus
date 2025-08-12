package rinha.restclient.config;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import rinha.restclient.Payments1RestClient;
import rinha.restclient.Payments2RestClient;
import rinha.restclient.SummaryRestClient;

import java.net.URI;

public class RestClientConfig {

    @Produces
    @ApplicationScoped
    public Payments1RestClient createPayments1RestClient(
            @ConfigProperty(name = "payments1.url") String url
    ) {
//        System.out.println("payments1.url: " + url);

        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(Payments1RestClient.class);
    }

    @Produces
    @ApplicationScoped
    public Payments2RestClient createPayments2RestClient(
            @ConfigProperty(name = "payments2.url") String url
    ) {
//        System.out.println("payments2.url: " + url);

        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(Payments2RestClient.class);
    }

    @Produces
    @ApplicationScoped
    public SummaryRestClient createSummaryRestClient(
            @ConfigProperty(name = "other.api") String url
    ) {
//        System.out.println("other.api: " + url);

        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(SummaryRestClient.class);
    }
}
