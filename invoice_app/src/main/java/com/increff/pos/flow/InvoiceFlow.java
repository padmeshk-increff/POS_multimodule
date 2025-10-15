package com.increff.pos.flow;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.enums.OrderStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Component
@Transactional(readOnly = true) // This operation is read-only, so we mark it as such for performance.
public class InvoiceFlow {

    @Autowired
    private OrderApi orderApi;
    @Autowired
    private OrderItemApi orderItemApi;

    public InvoiceData generateInvoice(Integer orderId) throws ApiException {
        // --- Step 1: Business Logic Validation ---
        // Fetch the necessary entities using the APIs from our pos_commons module.
        Order order = orderApi.getCheckById(orderId);

        // As per the requirement, we must validate the order status.
        if (order.getOrderStatus() != OrderStatus.INVOICED) {
            throw new ApiException("Invoice can only be generated for orders with status INVOICED.");
        }
        List<OrderItem> items = orderItemApi.getAllByOrderId(orderId);

        // --- Step 2: PDF Generation using Apache PDFBox ---
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // This is a basic example. You can build a much more detailed and styled layout.
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Invoice for Order #" + order.getId());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 650);
                contentStream.showText("Customer: " + order.getCustomerName());
                contentStream.endText();

                // You can loop through the 'items' list here to create a table of products.
            }

            // Save the generated document into a byte array in memory.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            pdfBytes = byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            // If PDF generation fails for any reason, throw a clean ApiException.
            throw new ApiException("Failed to generate PDF invoice: " + e.getMessage());
        }

        // --- Step 3: Base64 Encoding and DTO Creation ---
        // As per the requirement, encode the raw PDF bytes into a Base64 string.
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        InvoiceData invoiceData = new InvoiceData();
        invoiceData.setOrderId(orderId);
        invoiceData.setBase64Pdf(base64Pdf);

        return invoiceData;
    }
}

