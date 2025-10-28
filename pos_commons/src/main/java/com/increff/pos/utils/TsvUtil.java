package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.*;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.ProductUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TsvUtil {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private static final String TAB = "	"; // Tab character
    private static final String NL = "\n"; // New line character

    // Formatters
    private static final DateTimeFormatter ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    public static ConversionResult<String[]> validateAndParse(MultipartFile file, List<String> expectedHeaders) throws ApiException {
        validateFileMetadata(file);

        try {
            return parseAndValidateContent(file.getInputStream(), expectedHeaders);
        } catch (IOException e) {
            throw new ApiException("Could not read file content: " + e.getMessage());
        }
    }

    public static void normalizeRows(ConversionResult<String[]> tsvResult, List<String> expectedHeaders) {
        if (tsvResult == null || tsvResult.getValidRows() == null || expectedHeaders == null) {
            return; // Nothing to normalize or cannot determine barcode index
        }

        int barcodeIndex = -1;
        for (int i = 0; i < expectedHeaders.size(); i++) {
            if ("barcode".equalsIgnoreCase(expectedHeaders.get(i).trim())) {
                barcodeIndex = i;
                break;
            }
        }

        // Keep track of the final index for use in lambda
        final int finalBarcodeIndex = barcodeIndex;

        List<String[]> normalizedValidRows = tsvResult.getValidRows().stream()
                .map(row -> IntStream.range(0, row.length) // Use IntStream to get index
                        .mapToObj(i -> {
                            String cell = row[i];
                            if (cell == null) {
                                return null;
                            }
                            String trimmedCell = cell.trim();
                            // Convert to lowercase ONLY if it's NOT the barcode column
                            if (i != finalBarcodeIndex) {
                                return trimmedCell.toLowerCase();
                            } else {
                                return trimmedCell; // Keep barcode case as is (after trimming)
                            }
                        })
                        .toArray(String[]::new) // Collect back into a String array
                )
                .collect(Collectors.toList()); // Collect the normalized rows into a new list

        tsvResult.setValidRows(normalizedValidRows);
    }

    public static byte[] createProductUploadReport(
            ProductUploadResult uploadResult,
            List<ProductUploadRow> candidateRows,
            List<String> initialErrors
    ) throws ApiException {
        // Define the headers for the report file, including the new "status" column.
        final String[] headers = {"barcode", "name", "mrp", "clientName", "category", "status/error"};

        try {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 PrintWriter writer = new PrintWriter(out)) {

                // 1. Write the header row to the file.
                writer.println(String.join("\t", headers));

                // 2. Create a map of the rows that failed business logic for efficient lookup.
                //    The key is the original row number, and the value is the error message.
                Map<Integer, String> businessErrorMap = uploadResult.getFailedRows().stream()
                        .collect(Collectors.toMap(fail -> fail.getRow().getRowNumber(), FailedUploadRow::getErrorMessage));

                // 3. Process all the "candidate" rows (those that passed initial parsing).
                for (ProductUploadRow row : candidateRows) {
                    // For each row, check if it has a business error.
                    String status = businessErrorMap.getOrDefault(row.getRowNumber(), "SUCCESS");

                    // Write the original row data plus its final status to the report.
                    writer.printf("%s\t%s\t%s\t%s\t%s\t%s%n",
                            row.getBarcode(),
                            row.getName(),
                            row.getMrp(),
                            row.getClientName(),
                            row.getCategory(),
                            status);
                }

                // 4. Append any initial parsing errors to the end of the report.
                // These are for rows that were so malformed they were never even processed.
                if (!initialErrors.isEmpty()) {
                    writer.println("\n--- The following rows could not be parsed ---");
                    for (String error : initialErrors) {
                        writer.println(error);
                    }
                }

                writer.flush();
                return out.toByteArray();
            }
        } catch (IOException e) {
            // THE FIX: Catch the low-level IOException and wrap it in your custom ApiException.
            throw new ApiException("Failed to generate TSV report file: " + e.getMessage());
        }
    }

    public static byte[] createInventoryUploadReport(
            InventoryUploadResult uploadResult,
            List<InventoryUploadRow> candidateRows,
            List<String> initialErrors
    ) throws ApiException {
        // Define the headers for the inventory report file.
        final String[] headers = {"barcode", "quantity", "status/error"};

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            // 1. Write the header row.
            writer.println(String.join("\t", headers));

            // 2. Create a map of business logic errors for efficient lookup.
            Map<Integer, String> businessErrorMap = uploadResult.getFailedRows().stream()
                    .collect(Collectors.toMap(fail -> fail.getRow().getRowNumber(), FailedInventoryUploadRow::getErrorMessage));

            // 3. Process all candidate rows.
            for (InventoryUploadRow row : candidateRows) {
                // Get the status for the current row, defaulting to "SUCCESS".
                String status = businessErrorMap.getOrDefault(row.getRowNumber(), "SUCCESS");

                // Write the original data plus the status to the report.
                writer.printf("%s\t%s\t%s%n",
                        row.getBarcode(),
                        row.getQuantity(),
                        status);
            }

            // 4. Append any initial parsing errors from TsvUtil.
            if (!initialErrors.isEmpty()) {
                writer.println("\n--- The following rows could not be parsed ---");
                for (String error : initialErrors) {
                    writer.println(error);
                }
            }

            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            // Wrap the low-level exception in your application's custom exception.
            throw new ApiException("Failed to generate inventory TSV report file: " + e.getMessage());
        }
    }

    public static byte[] generateSalesReportTsv(SalesReportData data) {
        // ... (your existing implementation, ensure ZonedDateTime is used for dates) ...
        StringBuilder tsv = new StringBuilder();
        tsv.append("Sales Report Summary").append(NL);
        SalesReportData.SalesSummaryData summary = data.getSummary();
        tsv.append("Start Date").append(TAB).append(summary.getStartDate().format(ZONED_DATE_TIME_FORMATTER)).append(NL);
        tsv.append("End Date").append(TAB).append(summary.getEndDate().format(ZONED_DATE_TIME_FORMATTER)).append(NL);
        tsv.append("Total Revenue").append(TAB).append(summary.getTotalRevenue()).append(NL);
        tsv.append("Total Orders").append(TAB).append(summary.getTotalOrders()).append(NL);
        tsv.append("Average Order Value").append(TAB).append(summary.getAverageOrderValue()).append(NL);
        tsv.append("Total Items Sold").append(TAB).append(summary.getTotalItemsSold()).append(NL);

        tsv.append(NL).append("Sales Over Time").append(NL);
        tsv.append("Date").append(TAB).append("Revenue").append(NL);
        for (SalesReportData.SalesOverTimeData row : data.getSalesOverTime()) {
            // Assuming SalesOverTimeData.getDate() returns ZonedDateTime now
            tsv.append(row.getDate().format(ZONED_DATE_TIME_FORMATTER)).append(TAB);
            tsv.append(row.getRevenue()).append(NL);
        }

        tsv.append(NL).append("Product Performance").append(NL);
        tsv.append("Product ID").append(TAB)
                .append("Product Name").append(TAB)
                .append("Quantity Sold").append(TAB)
                .append("Total Revenue").append(NL);
        for (SummaryData.ProductSalesData row : data.getProductPerformance()) {
            tsv.append(escapeTsvField(row.getProductId())).append(TAB);
            tsv.append(escapeTsvField(row.getProductName())).append(TAB);
            tsv.append(escapeTsvField(row.getQuantitySold())).append(TAB);
            tsv.append(escapeTsvField(row.getTotalRevenue())).append(NL);
        }
        return tsv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateInventoryReportTsv(InventoryReportData data) {
        StringBuilder tsv = new StringBuilder();

        // --- Section 1: Summary ---
        tsv.append("Inventory Report Summary").append(NL);
        InventoryReportData.InventorySummaryData summary = data.getSummary();
        tsv.append("Report Generated At").append(TAB).append(summary.getReportGeneratedAt().format(ZONED_DATE_TIME_FORMATTER)).append(NL);
        tsv.append("Total Product SKUs").append(TAB).append(summary.getTotalProductSkus()).append(NL);
        tsv.append("Total Inventory Quantity").append(TAB).append(summary.getTotalInventoryQuantity()).append(NL);
        tsv.append("Total Inventory Value").append(TAB).append(summary.getTotalInventoryValue()).append(NL);
        tsv.append("Out of Stock Items").append(TAB).append(summary.getOutOfStockItems()).append(NL);
        tsv.append("Low Stock Items (<10)").append(TAB).append(summary.getLowStockItems()).append(NL);

        // --- Section 2: Inventory Items ---
        tsv.append(NL).append("Inventory Items").append(NL);
        // Header Row for items (matches InventoryItemData structure)
        tsv.append("Product ID").append(TAB)
                .append("Product Name").append(TAB)
                .append("Quantity").append(TAB)
                .append("Total Value").append(TAB)
                .append("Status").append(NL);

        // Data Rows for items
        for (InventoryReportData.InventoryItemData item : data.getItems()) {
            tsv.append(escapeTsvField(item.getProductId())).append(TAB);
            tsv.append(escapeTsvField(item.getProductName())).append(TAB);
            tsv.append(escapeTsvField(item.getQuantity())).append(TAB);
            tsv.append(escapeTsvField(item.getTotalValue())).append(TAB);
            tsv.append(escapeTsvField(item.getStatus())).append(NL);
        }

        return tsv.toString().getBytes(StandardCharsets.UTF_8);
    }


    private static void validateFileMetadata(MultipartFile file) throws ApiException {
        if (file.isEmpty()) {
            throw new ApiException("File is empty. Please upload a non-empty TSV file.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".tsv")) {
            throw new ApiException("Invalid file format. Only .tsv files are accepted.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException("File size exceeds the maximum limit of 5MB.");
        }
    }

    private static ConversionResult<String[]> parseAndValidateContent(InputStream inputStream, List<String> expectedHeaders) throws ApiException {
        List<String[]> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowNumber = 1; // Row number starts at 1 for the first data row

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            // Read and validate the header row
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ApiException("File is empty or does not contain a header row.");
            }
            validateHeader(headerLine, expectedHeaders);

            // Read and validate each data row by delegating to the helper
            String line;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                processDataRow(line, rowNumber, expectedHeaders.size(), validRows, errors);
            }
        } catch (IOException e) {
            throw new ApiException("Failed to read the uploaded file. Ensure it is a valid TSV and not corrupted.");
        }

        ConversionResult<String[]> result = new ConversionResult<>();
        result.setValidRows(validRows);
        result.setErrors(errors);
        return result;
    }

    private static void processDataRow(String line, int rowNumber, int expectedColumnCount, List<String[]> validRows, List<String> errors) {
        // Silently ignore blank lines
        if (line.trim().isEmpty()) {
            return;
        }

        String[] tokens = line.split("\t");

        // Check if the number of columns matches the number of headers.
        if (tokens.length != expectedColumnCount) {
            errors.add("Error in row #" + rowNumber + ": Invalid number of columns. Expected " + expectedColumnCount + ", but found " + tokens.length);
        } else {
            // If the structure is correct, add it to the list of valid rows.
            validRows.add(tokens);
        }
    }

    private static void validateHeader(String headerLine, List<String> expectedHeaders) throws ApiException {
        List<String> actualHeaders = Arrays.stream(headerLine.split("\t"))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<String> lowercasedExpected = expectedHeaders.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (!actualHeaders.equals(lowercasedExpected)) {
            throw new ApiException(
                    "Invalid file headers. Expected columns: [" + String.join(", ", expectedHeaders) +
                            "], but found: [" + String.join(", ", headerLine.split("\t")) + "]"
            );
        }
    }

    private static String escapeTsvField(Object field) {
        if (field == null) {
            return "";
        }
        // Replace literal tab and newline characters with spaces
        return String.valueOf(field).replace(TAB, " ").replace(NL, " ");
    }
}

