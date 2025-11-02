package com.increff.pos.unit.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.ReportFlow;
import com.increff.pos.model.data.InventoryReportData;
import com.increff.pos.model.data.SalesReportData;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.model.result.InventoryReportResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Behavior-focused unit tests for ReportFlow.
 * Complex report generation better tested via integration tests.
 */
public class ReportFlowTest {

    @Mock
    private OrderApi orderApi;
    @Mock
    private OrderItemApi orderItemApi;
    @Mock
    private ProductApi productApi;
    @Mock
    private InventoryApi inventoryApi;
    @InjectMocks
    private ReportFlow reportFlow;

    private ZonedDateTime testStart;
    private ZonedDateTime testEnd;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testStart = ZonedDateTime.now().minusDays(1);
        testEnd = ZonedDateTime.now();
    }

    // NOTE: Report generation involves complex utils and aggregations.
    // Full behavior better tested via integration tests.
    
    @Test
    public void getSummaryData_noErrors_shouldReturnData() throws ApiException {
        // GIVEN
        when(orderApi.getAllByDateRange(any(ZonedDateTime.class), any(ZonedDateTime.class))).thenReturn(Collections.emptyList());
        when(orderItemApi.getTopSellingProducts(any(ZonedDateTime.class), any(ZonedDateTime.class), any())).thenReturn(Collections.emptyList());
        when(inventoryApi.getLowStockItems(any(Integer.class))).thenReturn(Collections.emptyList());

        // WHEN
        SummaryData result = reportFlow.getSummaryData();

        // THEN
        assertNotNull(result);
    }

    @Test
    public void getSalesReport_validDates_shouldReturnReport() throws ApiException {
        // GIVEN
        when(orderApi.getAllByDateRange(testStart, testEnd)).thenReturn(Collections.emptyList());
        when(orderItemApi.getSalesByDate(testStart, testEnd)).thenReturn(Collections.emptyList());
        when(orderItemApi.getTopSellingProducts(testStart, testEnd, null)).thenReturn(Collections.emptyList());

        // WHEN
        SalesReportData result = reportFlow.getSalesReport(testStart, testEnd);

        // THEN
        assertNotNull(result);
    }

    @Test
    public void getInventoryReport_noErrors_shouldReturnReport() {
        // GIVEN
        when(inventoryApi.getInventoryReportData()).thenReturn(Collections.emptyList());

        // WHEN
        InventoryReportData result = reportFlow.getInventoryReport();

        // THEN
        assertNotNull(result);
    }
}