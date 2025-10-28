package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.ReportDto;
import com.increff.pos.model.data.SummaryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportDto reportDto;

    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    public SummaryData getSummary() throws ApiException {
        return reportDto.getSummary();
    }

    @RequestMapping(value = "/sales", method = RequestMethod.GET, produces = "text/tab-separated-values")
    public ResponseEntity<byte[]> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end)
            throws ApiException {
        return reportDto.getSalesReport(start,end);

    }

    @RequestMapping(value = "/inventory", method = RequestMethod.GET, produces = "text/tab-separated-values")
    public ResponseEntity<byte[]> getInventoryReport() throws ApiException {
        return reportDto.getInventoryReport();
    }

}
