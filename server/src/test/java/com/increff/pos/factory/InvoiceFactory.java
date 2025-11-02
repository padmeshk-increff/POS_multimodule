package com.increff.pos.factory;

import com.increff.pos.entity.Invoice;
import com.increff.pos.model.data.InvoiceData;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Test Data Factory for creating Invoice entities and InvoiceData objects using Instancio.
 */
public final class InvoiceFactory {

    private InvoiceFactory() {
    }

    /**
     * Model for a 'new' Invoice, not yet saved.
     * ID is null.
     */
    private static final Model<Invoice> NEW_INVOICE_MODEL = Instancio.of(Invoice.class)
            .set(field(Invoice::getId), null) // A new object has no ID
            .generate(field(Invoice::getOrderId), gen -> gen.ints().min(1))
            // THE FIX: Use .prefix(), .suffix(), etc.
            .generate(field(Invoice::getFilePath), gen -> gen.string()
                    .prefix("/tmp/invoices/inv-")
                    .alphaNumeric()
                    .length(8)
                    .suffix(".pdf")
            )
            .toModel();

    /**
     * Model for a 'persisted' Invoice, as if from the DB.
     * ID is a positive integer.
     */
    private static final Model<Invoice> PERSISTED_INVOICE_MODEL = Instancio.of(Invoice.class)
            .generate(field(Invoice::getId), gen -> gen.ints().min(1)) // Has an ID
            .generate(field(Invoice::getOrderId), gen -> gen.ints().min(1))
            // THE FIX: Use .prefix(), .suffix(), etc.
            .generate(field(Invoice::getFilePath), gen -> gen.string()
                    .prefix("/var/data/pos/invoices/inv-")
                    .alphaNumeric()
                    .length(8)
                    .suffix(".pdf")
            )
            .toModel();

    /**
     * Model for InvoiceData with realistic test data.
     */
    private static final Model<InvoiceData> INVOICE_DATA_MODEL = Instancio.of(InvoiceData.class)
            .generate(field(InvoiceData::getOrderId), gen -> gen.ints().min(1))
            .generate(field(InvoiceData::getBase64Pdf), gen -> gen.string()
                    .alphaNumeric()
                    .length(100, 500)
            )
            .toModel();

    /**
     * Creates a mock 'new' Invoice object for a specific order.
     * @param orderId The order ID to associate.
     * @return An Invoice object with a null ID.
     */
    public static Invoice mockNewObject(Integer orderId) {
        return Instancio.of(NEW_INVOICE_MODEL)
                .set(field(Invoice::getOrderId), orderId)
                .create();
    }

    /**
     * Creates a mock 'persisted' Invoice object for a specific order.
     * @param orderId The order ID to associate.
     * @return An Invoice object with a non-null ID.
     */
    public static Invoice mockPersistedObject(Integer orderId) {
        return Instancio.of(PERSISTED_INVOICE_MODEL)
                .set(field(Invoice::getOrderId), orderId)
                .create();
    }

    /**
     * Creates a mock 'persisted' Invoice object with a specific order ID and file path.
     * @param orderId The order ID to associate.
     * @param filePath The exact file path for testing.
     * @return An Invoice object with a non-null ID.
     */
    public static Invoice mockPersistedObject(Integer orderId, String filePath) {
        return Instancio.of(PERSISTED_INVOICE_MODEL)
                .set(field(Invoice::getOrderId), orderId)
                .set(field(Invoice::getFilePath), filePath)
                .create();
    }

    // --- InvoiceData Factory Methods ---




    /**
     * Creates a mock InvoiceData object for a specific order.
     * @param orderId The order ID to associate.
     * @return An InvoiceData object with generated base64Pdf.
     */
    public static InvoiceData mockNewDataObject(Integer orderId) {
        return Instancio.of(INVOICE_DATA_MODEL)
                .set(field(InvoiceData::getOrderId), orderId)
                .create();
    }

    /**
     * Creates a mock InvoiceData object with specific order ID and base64Pdf.
     * @param orderId The order ID to associate.
     * @param base64Pdf The base64-encoded PDF string.
     * @return An InvoiceData object with the specified values.
     */
    public static InvoiceData mockNewDataObject(Integer orderId, String base64Pdf) {
        return Instancio.of(INVOICE_DATA_MODEL)
                .set(field(InvoiceData::getOrderId), orderId)
                .set(field(InvoiceData::getBase64Pdf), base64Pdf)
                .create();
    }
}