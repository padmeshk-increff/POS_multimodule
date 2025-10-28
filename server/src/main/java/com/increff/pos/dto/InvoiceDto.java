package com.increff.pos.dto;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.utils.ResponseEntityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class InvoiceDto extends AbstractDto{

        @Autowired
        private InvoiceFlow invoiceFlow;

        @Autowired
        private InvoiceApi invoiceApi;

        @Autowired
        private RestTemplate restTemplate;

        @Value("${invoice.app.url}")
        private String invoiceAppUrl;

        public Map<String, String> generateAndStoreInvoice(Integer orderId) throws ApiException {
            InvoiceForm invoiceForm = invoiceFlow.generateInvoiceForm(orderId);

            String url = invoiceAppUrl + "/generate";
            InvoiceData invoiceData = callInvoiceApp(url,invoiceForm);
            invoiceFlow.storeInvoiceData(invoiceData);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Invoice generated and stored successfully for order ID: " + orderId);
            return response;
        }

        public ResponseEntity<byte[]> getInvoicePdf(Integer orderId) throws ApiException {
            byte[] pdfBytes = invoiceApi.getInvoicePdfBytes(orderId);

            String fileName = "invoice-order-" + orderId + ".pdf";
            return ResponseEntityUtil.buildPdfResponse(pdfBytes, fileName);
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

