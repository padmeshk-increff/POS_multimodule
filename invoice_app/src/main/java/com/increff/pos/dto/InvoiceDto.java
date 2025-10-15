package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvoiceDto {

    @Autowired
    private InvoiceFlow invoiceFlow;

    public InvoiceData generateInvoice(Integer orderId) throws ApiException {
        // The DTO's job is to orchestrate the call to the Flow layer.
        return invoiceFlow.generateInvoice(orderId);
    }
}
