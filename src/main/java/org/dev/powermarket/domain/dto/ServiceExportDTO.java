package org.dev.powermarket.domain.dto;



import lombok.Getter;
import lombok.Setter;
import org.dev.powermarket.domain.Service;
import org.dev.powermarket.domain.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Setter
@Getter
public class ServiceExportDTO {


    private String title;
    private String description;
    private ServiceCategory category;
    private BigDecimal price;
    private OffsetDateTime createdAt;



    public ServiceExportDTO(Service lot) {
        this.title = lot.getTitle();
        this.description = lot.getDescription();
        this.category = lot.getCategory();
        this.price = lot.getPricePerDay();

        //todo реализовать нормальный превод к текущему времени
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        this.createdAt = lot.getCreatedAt().atOffset(offset);

    }


    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category.toString();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
