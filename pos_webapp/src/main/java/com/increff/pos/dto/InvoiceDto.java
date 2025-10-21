package com.increff.pos.dto;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.utils.ResponseEntityUtil; // We will create this utility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class InvoiceDto {

    @Autowired
    private InvoiceFlow invoiceFlow;

    @Autowired
    private InvoiceApi invoiceApi;

    public Map<String, String> generateAndStoreInvoice(Integer orderId) throws ApiException {
        invoiceFlow.generateAndStoreInvoice(orderId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice generated and stored successfully for order ID: " + orderId);
        return response;
    }

    public ResponseEntity<byte[]> getInvoicePdf(Integer orderId) throws ApiException {
        byte[] pdfBytes = invoiceApi.getInvoicePdfBytes(orderId);

        String fileName = "invoice-order-" + orderId + ".pdf";
        return ResponseEntityUtil.buildPdfResponse(pdfBytes, fileName);
    }
}

