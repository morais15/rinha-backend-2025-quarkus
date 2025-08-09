package rinha.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentsRequest {
    public UUID correlationId;
    public BigDecimal amount;
}
