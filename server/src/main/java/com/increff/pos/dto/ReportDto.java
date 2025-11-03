package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.ReportFlow;
import com.increff.pos.model.data.InventoryReportData;
import com.increff.pos.model.data.SalesReportData;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.utils.ResponseEntityUtil;
import com.increff.pos.utils.TsvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ReportDto {

    @Autowired
    private ReportFlow reportFlow;

    public SummaryData getSummary() throws ApiException {
        return reportFlow.getSummaryData();
    }

    public ResponseEntity<byte[]> getSalesReport(ZonedDateTime start, ZonedDateTime end) throws ApiException{
        SalesReportData salesReportData = reportFlow.getSalesReport(start,end);

        byte[] reportBytes = TsvUtil.generateSalesReportTsv(salesReportData);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String fileName = "sales-report-" + start.format(formatter) + "-to-" + end.format(formatter) + ".tsv";

        return ResponseEntityUtil.buildTsvResponse(reportBytes,fileName);
    }

    public ResponseEntity<byte[]> getInventoryReport() throws ApiException{
        InventoryReportData inventoryReportData = reportFlow.getInventoryReport();

        byte[] reportBytes = TsvUtil.generateInventoryReportTsv(inventoryReportData);

        return ResponseEntityUtil.buildTsvResponse(reportBytes, "inventory-report.tsv");
    }
}
