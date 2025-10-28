package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.InvoiceDto;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class InvoiceController {

    @Autowired
    private InvoiceDto invoiceDto;

    @RequestMapping(value = "/generate", method = RequestMethod.POST)
    public InvoiceData generateInvoice(@RequestBody InvoiceForm form) throws ApiException {
        return invoiceDto.generateInvoice(form);
    }
}

