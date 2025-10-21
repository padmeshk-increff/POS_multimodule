package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.utils.InvoiceGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
public class InvoiceController {

    @RequestMapping(value = "/generate", method = RequestMethod.POST)
    public InvoiceData generateInvoice(@RequestBody InvoiceForm form) throws ApiException {
        return InvoiceGenerator.generateInvoice(form);
    }
}

