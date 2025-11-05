package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.model.form.InvoiceItemForm;

// FOP and XSLT imports
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

public class InvoiceGenerator {

    // Keep formatters, as we need to format data *before* sending it
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final String CURRENCY_SYMBOL = "Rs.";

    // FOP factory is thread-safe and can be reused
    private static final FopFactory FOP_FACTORY = FopFactory.newInstance(new java.io.File(".").toURI());

    public static String generateInvoice(InvoiceForm form) throws ApiException {
        try {
            byte[] pdfBytes = generatePdfBytes(form);
            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (Exception e) { // Catch more general FOP/Transformer exceptions
            throw new ApiException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private static byte[] generatePdfBytes(InvoiceForm form) throws Exception {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 1. Setup FOP with PDF output
            Fop fop = FOP_FACTORY.newFop(MimeConstants.MIME_PDF, out);

            // 2. Load the XSLT template
            InputStream templateStream = InvoiceGenerator.class.getClassLoader()
                    .getResourceAsStream("invoice-template.xsl");
            if (templateStream == null) {
                throw new IOException("Invoice template 'invoice-template.xsl' not found in resources.");
            }
            Source xsltSrc = new StreamSource(templateStream);

            // 3. Setup the XSLT Transformer (using Saxon)
            // We explicitly tell it to use Saxon
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            // 4. Set all parameters for the XSLT
            transformer.setParameter("orderId", String.valueOf(form.getOrderId()));
            transformer.setParameter("orderDate", DATE_FORMATTER.format(
                    form.getOrderDate() != null ? form.getOrderDate() : ZonedDateTime.now()
            ));

            String customerName = (form.getCustomerName() == null || form.getCustomerName().trim().isEmpty())
                    ? "N/A" : form.getCustomerName();
            String customerPhone = (form.getCustomerPhone() == null || form.getCustomerPhone().trim().isEmpty())
                    ? "N/A" : form.getCustomerPhone();

            transformer.setParameter("customerName", customerName);
            transformer.setParameter("customerPhone", customerPhone);
            transformer.setParameter("totalAmount", CURRENCY_FORMAT.format(form.getTotalAmount()));
            transformer.setParameter("currencySymbol", CURRENCY_SYMBOL);

            // 5. Convert the item list to an XML string
            String itemsXml = convertItemsToXml(form.getItems());
            transformer.setParameter("itemsXml", itemsXml);

            // 6. Create a "dummy" XML source, since all our data is in params
            Source xmlSrc = new StreamSource(new StringReader("<dummy/>"));

            // 7. Define the output, which is the FOP processor
            Result res = new SAXResult(fop.getDefaultHandler());

            // 8. Run the transformation!
            // XSLT processor will read template, merge params, and pipe
            // the resulting XSL-FO into FOP, which generates the PDF.
            transformer.transform(xmlSrc, res);

            // 9. Return the PDF bytes
            return out.toByteArray();
        }
    }

    /**
     * Helper method to convert the item list into a simple XML string
     * that our XSLT can understand and parse.
     */
    private static String convertItemsToXml(List<InvoiceItemForm> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<items>");
        if (items != null) {
            for (InvoiceItemForm item : items) {
                sb.append("<item>");

                sb.append("<productName>").append(escapeXml(item.getProductName())).append("</productName>");
                sb.append("<quantity>").append(item.getQuantity()).append("</quantity>");
                sb.append("<sellingPrice>").append(CURRENCY_FORMAT.format(item.getSellingPrice())).append("</sellingPrice>");

                double total = item.getQuantity() * item.getSellingPrice();
                sb.append("<total>").append(CURRENCY_FORMAT.format(total)).append("</total>");

                sb.append("</item>");
            }
        }
        sb.append("</items>");
        return sb.toString();
    }

    /**
     * Basic XML escaping for data.
     */
    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}