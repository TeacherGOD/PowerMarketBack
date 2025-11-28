package org.dev.powermarket.web;


import org.dev.powermarket.service.ServiceExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceExportController {

    private final ServiceExportService serviceExportService;

    @Autowired
    public ServiceExportController(ServiceExportService serviceExportService) {
        this.serviceExportService = serviceExportService;
    }


    //todo в целом улучшить содержимое файла.
    @GetMapping("/export/period")
    public ResponseEntity<byte[]> getLotsToExelByPeriod(@RequestParam UUID supplierId,
                                                        @RequestParam(required = false) OffsetDateTime from,
                                                        @RequestParam(required = false) OffsetDateTime to) throws IOException {
        byte[] excel = serviceExportService.getLotsToExelByPeriod(supplierId, from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=myLots.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }


}
