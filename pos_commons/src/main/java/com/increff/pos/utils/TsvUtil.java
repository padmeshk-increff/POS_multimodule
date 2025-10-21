package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.FailedUploadRow;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.result.InventoryUploadResult;
import com.increff.pos.model.data.InventoryUploadRow;
import com.increff.pos.model.data.FailedInventoryUploadRow;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.ProductUploadResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TsvUtil {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    public static ConversionResult<String[]> validateAndParse(MultipartFile file, List<String> expectedHeaders) throws ApiException {
        validateFileMetadata(file);

        try {
            return parseAndValidateContent(file.getInputStream(), expectedHeaders);
        } catch (IOException e) {
            throw new ApiException("Could not read file content: " + e.getMessage());
        }
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
}

