package org.dev.powermarket.service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.dev.powermarket.domain.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateServiceRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000)
    private String description;

    @NotNull(message = "Category is required")
    private ServiceCategory category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal pricePerDay;

    @NotBlank(message = "Location is required")
    private String location;

    @Min(1)
    private String capacity;

    private String availableCapacity;

    private String technicalSpecs;

    @NotEmpty(message = "At least one availability period is required")
    private List<AvailabilityPeriod> availabilities;

    @Data
    public static class AvailabilityPeriod {
        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;
    }


}
