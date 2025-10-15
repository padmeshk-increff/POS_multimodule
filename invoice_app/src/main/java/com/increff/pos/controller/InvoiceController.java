package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceDto invoiceDto;

    /**
     * This endpoint generates the invoice data for a given order ID.
     * It returns a JSON object containing the Base64 encoded PDF string.
     */
    //TODO: make it stateless, shouldn't access db
    //TODO: scheduler,security,invoice,ui
    @RequestMapping(value = "/api/invoices/{orderId}", method = RequestMethod.POST)
    public InvoiceData generateInvoice(@PathVariable("orderId") Integer orderId) throws ApiException {
        // Delegate all logic to the DTO layer
        return invoiceDto.generateInvoice(orderId);
    }
}

