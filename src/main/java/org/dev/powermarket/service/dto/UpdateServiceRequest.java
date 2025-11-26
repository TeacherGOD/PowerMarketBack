package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateServiceRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal pricePerDay;

    private String location;

    @Min(1)
    private String capacity;

    private String technicalSpecs;

    private Boolean active;
}
