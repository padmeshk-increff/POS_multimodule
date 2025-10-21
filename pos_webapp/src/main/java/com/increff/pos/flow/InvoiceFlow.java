package com.increff.pos.flow;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Invoice;
import com.increff.pos.entity.Order;
import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.utils.InvoiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = ApiException.class)
public class InvoiceFlow {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderApi orderApi;
    @Autowired
    private OrderItemApi orderItemApi;
    @Autowired
    private ProductApi productApi;
    @Autowired
    private InvoiceApi invoiceApi;

    @Value("${invoice.app.url}")
    private String invoiceAppUrl;
    @Value("${invoice.storage.path}")
    private String invoiceStoragePath;

    public void generateAndStoreInvoice(Integer orderId) throws ApiException {
        // --- Step 1: Perform Business Validations by calling the API layers ---
        invoiceApi.checkInvoiceDoesNotExist(orderId);
        Order order = orderApi.updateInvoiceOrder(orderId);

        // --- Step 2: Gather necessary data by calling the API layers ---
        List<OrderItem> items = orderItemApi.getAllByOrderId(orderId);
        List<Integer> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toList());
        Map<Integer, Product> productMap = productApi.getByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // --- Step 3: Create the data package and call the external invoice microservice ---
        InvoiceForm form = InvoiceUtil.createInvoiceForm(order, items, productMap);
        String url = invoiceAppUrl + "/generate";
        InvoiceData invoiceData = callInvoiceApp(url, form);

        // --- Step 4: Save the file and create the final database record ---
        String filePath = savePdfToFile(invoiceData.getBase64Pdf(), orderId);
        Invoice invoice = InvoiceUtil.createInvoiceEntity(orderId, filePath);
        invoiceApi.insert(invoice);

        orderApi.updateInvoicePathById(orderId,filePath);
    }

    private String savePdfToFile(String base64Pdf, Integer orderId) throws ApiException {
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

    private InvoiceData callInvoiceApp(String url, InvoiceForm form) throws ApiException {
        try {
            return restTemplate.postForObject(url, form, InvoiceData.class);
        } catch (ResourceAccessException e) {
            throw new ApiException("The invoice generation service is currently unavailable. Please try again later.");
        } catch (HttpClientErrorException e) {
            throw new ApiException("Invoice generation service failed: " + e.getResponseBodyAsString());
        }
    }
}

