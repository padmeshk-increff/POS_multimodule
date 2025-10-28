package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Invoice;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.model.form.InvoiceItemForm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InvoiceUtil {

    public static InvoiceData convert(String base64Pdf, Integer orderId){
        InvoiceData invoiceData = new InvoiceData();
        invoiceData.setOrderId(orderId);
        invoiceData.setBase64Pdf(base64Pdf);
        return invoiceData;
    }

    public static InvoiceForm createInvoiceForm(Order order, List<OrderItem> items, Map<Integer, Product> productMap) {
        InvoiceForm form = new InvoiceForm();
        form.setOrderId(order.getId());
        form.setOrderDate(order.getCreatedAt());
        form.setCustomerName(order.getCustomerName());
        form.setCustomerPhone(order.getCustomerPhone());
        form.setTotalAmount(order.getTotalAmount());

        List<InvoiceItemForm> itemForms = items.stream().map(item -> {
            InvoiceItemForm itemForm = new InvoiceItemForm();
            Product product = productMap.get(item.getProductId());
            itemForm.setProductName(product != null ? product.getName() : "Unknown Product");
            itemForm.setBarcode(product != null ? product.getBarcode() : "N/A");
            itemForm.setQuantity(item.getQuantity());
            itemForm.setMrp(product != null ? product.getMrp() : 0.0);
            itemForm.setSellingPrice(item.getSellingPrice());
            return itemForm;
        }).collect(Collectors.toList());
        form.setItems(itemForms);
        return form;
    }

    public static Invoice createInvoiceEntity(Integer orderId, String filePath) {
        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setFilePath(filePath);
        return invoice;
    }

    public static String savePdfToFile(String base64Pdf, Integer orderId,String invoiceStoragePath) throws ApiException {
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

    public static Map<Integer,Product> mapByProductIds(List<Product> products){
        return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
