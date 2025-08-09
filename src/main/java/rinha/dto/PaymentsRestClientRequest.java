package rinha.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class PaymentsRestClientRequest {
    public UUID correlationId;
    public BigDecimal amount;
    public Instant requestedAt;
}
