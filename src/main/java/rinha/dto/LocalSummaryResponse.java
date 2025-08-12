package rinha.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class LocalSummaryResponse {
    public Long defaultReqs;
    public Long fallbackReqs;
}
