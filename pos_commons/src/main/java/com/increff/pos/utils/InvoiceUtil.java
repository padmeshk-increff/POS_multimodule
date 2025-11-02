package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Utility class for Invoice-related operations that DO NOT depend on entity classes.
 * This class is in pos_commons and can be used by both server and invoice_app modules.
 * 
 * For entity-dependent operations, see InvoiceHelper in the server module.
 */
public class InvoiceUtil {

    /**
     * Converts base64 PDF string and order ID into InvoiceData object.
     * This method only uses DTOs from pos_commons, no entity dependencies.
     */
    public static InvoiceData convert(String base64Pdf, Integer orderId){
        InvoiceData invoiceData = new InvoiceData();
        invoiceData.setOrderId(orderId);
        invoiceData.setBase64Pdf(base64Pdf);
        return invoiceData;
    }

    /**
     * Saves a base64-encoded PDF to the file system.
     * Pure file I/O logic with no entity dependencies.
     */
    public static String savePdfToFile(String base64Pdf, Integer orderId, String invoiceStoragePath) throws ApiException {
        try {
            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
            Path directory = Paths.get(invoiceStoragePath);
            Files.createDirectories(directory);
            String filename = "invoice-order-" + orderId + ".pdf";
            Path fullPath = directory.resolve(filename);
            Files.write(fullPath, pdfBytes);
            return fullPath.toString();
        } catch (IOException | IllegalArgumentException e) {
            throw new ApiException("Failed to save the generated PDF file locally: " + e.getMessage());
        }
    }
}
