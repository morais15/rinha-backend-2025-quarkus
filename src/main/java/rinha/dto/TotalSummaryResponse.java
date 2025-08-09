package rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class TotalSummaryResponse {

    @JsonProperty("default")
    public SummaryResponse defaultSummary;

    @JsonProperty("fallback")
    public SummaryResponse fallbackSummary;
}
