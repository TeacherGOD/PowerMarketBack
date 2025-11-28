package org.dev.powermarket.service;


import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dev.powermarket.domain.dto.ServiceExportDTO;
import org.dev.powermarket.repository.ServiceExportRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceExportService {

    private final ServiceExportRepository servicesRepository;



    public byte[] getLotsToExelByPeriod(UUID supplierId, OffsetDateTime from , OffsetDateTime to)
            throws IOException{
        Instant fromInstant;
        Instant toInstant;
        if (from == null)
            fromInstant = Instant.from(OffsetDateTime.parse("1970-01-01T00:00:00Z"));
        else {
            fromInstant = from.toInstant();
        }
        if (to==null) toInstant = Instant.now();
        else
        {
            toInstant = to.toInstant();
        }
        List<org.dev.powermarket.domain.Service> response = servicesRepository.getLotsByCreatedAt(supplierId, fromInstant, toInstant);
        List<ServiceExportDTO> responseDTO = new java.util.ArrayList<>();
        for (org.dev.powermarket.domain.Service lot : response) {
            responseDTO.add(new ServiceExportDTO(lot));
        }


        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Лоты");

            // заголовок
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Заголовок");
            header.createCell(1).setCellValue("Описание");
            header.createCell(2).setCellValue("Категория"); //todo так же категории скасты энумов в текст
            header.createCell(3).setCellValue("Цена");
            header.createCell(4).setCellValue("Дата создания");


            int rowIdx = 1;
            for (ServiceExportDTO lot : responseDTO) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(lot.getTitle());
                row.createCell(1).setCellValue(lot.getDescription());
                row.createCell(2).setCellValue(lot.getCategory());
                if (lot.getPrice()!=null){
                    row.createCell(3).setCellValue(lot.getPrice().toString());
                }
                else {
                    row.createCell(3).setCellValue(0);
                }
                if (lot.getCreatedAt()!=null){
                    row.createCell(4).setCellValue(lot.getCreatedAt().toString());
                }
                else {
                    row.createCell(3).setCellValue("N/A");
                }

            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
