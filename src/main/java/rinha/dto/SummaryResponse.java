package rinha.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
public class SummaryResponse {
    public Integer totalRequests;
    public BigDecimal totalAmount;
}
