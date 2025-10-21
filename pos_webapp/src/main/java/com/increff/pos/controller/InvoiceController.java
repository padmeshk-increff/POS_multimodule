package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.InvoiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceDto invoiceDto;

    @RequestMapping(value = "/invoices/{orderId}", method = RequestMethod.POST)
    public Map<String, String> generateAndStoreInvoice(@PathVariable("orderId") Integer orderId) throws ApiException {
        return invoiceDto.generateAndStoreInvoice(orderId);
    }

    @RequestMapping(value = "/invoices/{orderId}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("orderId") Integer orderId) throws ApiException {
        return invoiceDto.getInvoicePdf(orderId);
    }
}
