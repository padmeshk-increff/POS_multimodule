package com.increff.pos.unit.api;

import com.increff.pos.api.InvoiceApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.InvoiceDao;
import com.increff.pos.entity.Invoice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.increff.pos.factory.InvoiceFactory.mockNewObject;
import static com.increff.pos.factory.InvoiceFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;


public class InvoiceApiTest {

    @Mock
    private InvoiceDao invoiceDao;

    @InjectMocks
    private InvoiceApi invoiceApi;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------------------
    // insert()
    // ---------------------------------------------------------------------

    @Test
    public void insert_validInvoice_returnsSameInstance() throws ApiException {
        Invoice invoice = mockNewObject(1);
        when(invoiceDao.selectByOrderId(1)).thenReturn(null);

        Invoice result = invoiceApi.insert(invoice);

        assertSame(invoice, result);
    }

    @Test
    public void insert_nullInvoice_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.insert(null)
        );
        assertEquals("Invoice object cannot be null", ex.getMessage());
    }

    @Test
    public void insert_nullOrderId_throwsException() {
        Invoice invoice = mockNewObject(1);
        invoice.setOrderId(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.insert(invoice)
        );
        assertEquals("Order ID cannot be null in Invoice", ex.getMessage());
    }

    @Test
    public void insert_emptyFilePath_throwsException() {
        Invoice invoice = mockNewObject(1);
        invoice.setFilePath("   ");

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.insert(invoice)
        );
        assertEquals("File path cannot be empty in an invoice record", ex.getMessage());
    }

    @Test
    public void insert_duplicateInvoice_throwsException() {
        Invoice invoice = mockNewObject(5);
        when(invoiceDao.selectByOrderId(5)).thenReturn(mockPersistedObject(5));

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.insert(invoice)
        );
        assertEquals("An invoice for order ID 5 has already been generated.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getCheckByOrderId()
    // ---------------------------------------------------------------------

    @Test
    public void getCheckByOrderId_existingOrder_returnsInvoice() throws ApiException {
        Invoice existing = mockPersistedObject(2);
        when(invoiceDao.selectByOrderId(2)).thenReturn(existing);

        Invoice result = invoiceApi.getCheckByOrderId(2);

        assertSame(existing, result);
    }

    @Test
    public void getCheckByOrderId_nullId_throwsException() {
        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.getCheckByOrderId(null)
        );
        assertEquals("Order ID cannot be null", ex.getMessage());
    }

    @Test
    public void getCheckByOrderId_missingInvoice_throwsException() {
        when(invoiceDao.selectByOrderId(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.getCheckByOrderId(404)
        );
        assertEquals("Invoice with Order ID 404 doesn't not exist", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // checkInvoiceDoesNotExist()
    // ---------------------------------------------------------------------

    @Test
    public void checkInvoiceDoesNotExist_invoiceMissing_allowsInsertion() throws ApiException {
        when(invoiceDao.selectByOrderId(3)).thenReturn(null);

        invoiceApi.checkInvoiceDoesNotExist(3);
    }

    @Test
    public void checkInvoiceDoesNotExist_duplicateInvoice_throwsException() {
        when(invoiceDao.selectByOrderId(3)).thenReturn(mockPersistedObject(3));

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.checkInvoiceDoesNotExist(3)
        );
        assertEquals("An invoice for order ID 3 has already been generated.", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // getInvoicePdfBytes()
    // ---------------------------------------------------------------------

    @Test
    public void getInvoicePdfBytes_existingInvoice_returnsPdfBytes() throws ApiException {
        Invoice invoice = mockPersistedObject(10, "/tmp/invoice.pdf");
        when(invoiceDao.selectByOrderId(10)).thenReturn(invoice);
        byte[] fileBytes = "PDF".getBytes();

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            Path path = Mockito.mock(Path.class);
            mockedPaths.when(() -> Paths.get(invoice.getFilePath())).thenReturn(path);
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
            mockedFiles.when(() -> Files.readAllBytes(path)).thenReturn(fileBytes);

            byte[] result = invoiceApi.getInvoicePdfBytes(10);

            assertArrayEquals(fileBytes, result);
        }
    }

    @Test
    public void getInvoicePdfBytes_missingInvoice_throwsException() {
        when(invoiceDao.selectByOrderId(99)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
            () -> invoiceApi.getInvoicePdfBytes(99)
        );
        assertEquals("Invoice with Order ID 99 doesn't not exist", ex.getMessage());
    }

    @Test
    public void getInvoicePdfBytes_missingFileOnDisk_throwsException() {
        Invoice invoice = mockPersistedObject(11, "/missing/invoice.pdf");
        when(invoiceDao.selectByOrderId(11)).thenReturn(invoice);

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            Path path = Mockito.mock(Path.class);
            mockedPaths.when(() -> Paths.get(invoice.getFilePath())).thenReturn(path);
            mockedFiles.when(() -> Files.exists(path)).thenReturn(false);

            ApiException ex = assertThrows(ApiException.class,
                () -> invoiceApi.getInvoicePdfBytes(11)
            );
            assertTrue(ex.getMessage().startsWith("Invoice file could not be found"));
        }
    }

    @Test
    public void getInvoicePdfBytes_readFailure_throwsException() throws IOException {
        Invoice invoice = mockPersistedObject(12, "/corrupt/invoice.pdf");
        when(invoiceDao.selectByOrderId(12)).thenReturn(invoice);

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            Path path = Mockito.mock(Path.class);
            mockedPaths.when(() -> Paths.get(invoice.getFilePath())).thenReturn(path);
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
            mockedFiles.when(() -> Files.readAllBytes(path)).thenThrow(new IOException("Disk read error"));

            ApiException ex = assertThrows(ApiException.class,
                () -> invoiceApi.getInvoicePdfBytes(12)
            );
            assertEquals("Failed to read the invoice file from disk: Disk read error", ex.getMessage());
        }
    }
}