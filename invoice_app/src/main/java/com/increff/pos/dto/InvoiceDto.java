package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.utils.InvoiceGenerator;
import com.increff.pos.utils.InvoiceUtil;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDto {

    public InvoiceData generateInvoice(InvoiceForm invoiceForm) throws ApiException {
        String base64Pdf = InvoiceGenerator.generateInvoice(invoiceForm);

        return InvoiceUtil.convert(base64Pdf, invoiceForm.getOrderId());
    }
}
