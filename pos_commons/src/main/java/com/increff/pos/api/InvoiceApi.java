package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.InvoiceDao;
import com.increff.pos.entity.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional(rollbackFor = ApiException.class)
public class InvoiceApi extends AbstractApi {

    @Autowired
    private InvoiceDao invoiceDao;

    public Invoice insert(Invoice invoice) throws ApiException {
        checkNull(invoice, "Invoice object cannot be null");

        if (invoice.getFilePath() == null || invoice.getFilePath().trim().isEmpty()) {
            throw new ApiException("File path cannot be empty in an invoice record");
        }

        invoiceDao.insert(invoice);
        return invoice;
    }

    public Invoice getByOrderId(Integer orderId) throws ApiException {
        checkNull(orderId, "Order ID cannot be null");

        return invoiceDao.selectByOrderId(orderId);
    }

    public void checkInvoiceDoesNotExist(Integer orderId) throws ApiException {
        Invoice existingInvoice = getByOrderId(orderId);
        if (existingInvoice != null) {
            throw new ApiException("An invoice for order ID " + orderId + " has already been generated.");
        }
    }

    public byte[] getInvoicePdfBytes(Integer orderId) throws ApiException {
        Invoice invoice = getByOrderId(orderId);
        if (invoice == null) {
            throw new ApiException("No invoice has been generated for order ID: " + orderId);
        }

        Path filePath = Paths.get(invoice.getFilePath());
        if (!Files.exists(filePath)) {
            throw new ApiException("Invoice file could not be found on the server at path: " + filePath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ApiException("Failed to read the invoice file from disk: " + e.getMessage());
        }
    }
}
