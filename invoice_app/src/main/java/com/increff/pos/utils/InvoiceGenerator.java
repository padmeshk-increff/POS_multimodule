package com.increff.pos.utils;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceForm;
import com.increff.pos.model.form.InvoiceItemForm;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class InvoiceGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final String CURRENCY_SYMBOL = "Rs.";

    // --- Page & Layout Constants ---
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50f;
    private static final float PAGE_TOP = PAGE_SIZE.getHeight() - MARGIN;
    private static final float BOTTOM_MARGIN = 50f;
    private static final float USABLE_WIDTH = PAGE_SIZE.getWidth() - (2 * MARGIN);

    // --- Font & Color Constants ---
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDFont FONT_REGULAR = PDType1Font.HELVETICA;
    private static final PDFont FONT_ITALIC = PDType1Font.HELVETICA_OBLIQUE;
    private static final Color COLOR_PRIMARY_TEXT = new Color(17, 24, 39);
    private static final Color COLOR_SECONDARY_TEXT = new Color(107, 114, 128);
    private static final Color COLOR_ACCENT = new Color(59, 130, 246);
    private static final Color COLOR_HEADER_BG = new Color(31, 41, 55); // Dark gray-blue
    private static final Color COLOR_HEADER_TEXT = Color.WHITE;
    private static final Color COLOR_TABLE_HEADER_BG = new Color(243, 244, 246);
    private static final Color COLOR_TABLE_BORDER = new Color(229, 231, 235);
    private static final Color COLOR_ROW_ALT = new Color(249, 250, 251);

    public static String generateInvoice(InvoiceForm form) throws ApiException {
        try {
            byte[] pdfBytes = generatePdfBytes(form);
            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (IOException e) {
            throw new ApiException("Failed to generate PDF byte stream: " + e.getMessage());
        }
    }

    private static byte[] generatePdfBytes(InvoiceForm form) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PageRenderer renderer = new PageRenderer(document, form);
            renderer.render();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ---------- Inner helper class to manage rendering across multiple pages ----------
    private static class PageRenderer {
        private final PDDocument doc;
        private PDPageContentStream cs;
        private float cursorY;
        private final InvoiceForm form;

        PageRenderer(PDDocument doc, InvoiceForm form) {
            this.doc = doc;
            this.form = form;
        }

        void render() throws IOException {
            startNewPage();
            drawHeader();
            drawCustomerInfo();
            drawTable(form.getItems());
            drawTotals();
            drawFooter();
            closeCurrentStream();
        }

        private void startNewPage() throws IOException {
            if (cs != null) {
                closeCurrentStream();
            }
            PDPage page = new PDPage(PAGE_SIZE);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            cursorY = PAGE_TOP;
        }

        private void closeCurrentStream() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        private void drawHeader() throws IOException {
            // Full-width colored header bar
            cs.setNonStrokingColor(COLOR_HEADER_BG);
            cs.addRect(0, 750, PAGE_SIZE.getWidth(), 60);
            cs.fill();

            // "Increff POS" on the left
            cs.setFont(FONT_BOLD, 20);
            cs.setNonStrokingColor(COLOR_HEADER_TEXT);
            drawText("Increff POS", cs, MARGIN, 770);

            // "INVOICE" on the right
            cs.setFont(FONT_BOLD, 36);
            drawTextRightAligned("INVOICE", cs, FONT_BOLD, 36, MARGIN + USABLE_WIDTH, 765);

            cursorY = 720;
        }

        private void drawCustomerInfo() throws IOException {
            // Right-aligned order details
            cs.setFont(FONT_REGULAR, 9);
            cs.setNonStrokingColor(COLOR_SECONDARY_TEXT);
            drawTextRightAligned("Order ID: #" + form.getOrderId(), cs, FONT_REGULAR, 9, MARGIN + USABLE_WIDTH, cursorY);
            drawTextRightAligned("Date: " + DATE_FORMATTER.format(form.getOrderDate() != null ? form.getOrderDate() : ZonedDateTime.now()), cs, FONT_REGULAR, 9, MARGIN + USABLE_WIDTH, cursorY - 12);

            // Left-aligned customer details
            cs.setFont(FONT_BOLD, 10);
            cs.setNonStrokingColor(COLOR_SECONDARY_TEXT);
            drawText("BILL TO", cs, MARGIN, cursorY);

            cs.setFont(FONT_BOLD, 12);
            cs.setNonStrokingColor(COLOR_PRIMARY_TEXT);
            drawText(form.getCustomerName() != null && !form.getCustomerName().isEmpty()? form.getCustomerName() : "N/A", cs, MARGIN, cursorY - 15);

            cs.setFont(FONT_REGULAR, 10);
            cs.setNonStrokingColor(COLOR_SECONDARY_TEXT);
            drawText("Phone: " + (form.getCustomerPhone() != null && !form.getCustomerPhone().isEmpty() ? form.getCustomerPhone() : "N/A"), cs, MARGIN, cursorY - 30);

            cursorY -= 60;
        }

        private void drawTable(List<InvoiceItemForm> items) throws IOException {
            float tableTopY = cursorY;

            drawTableHeader();
            cursorY -= 25; // Height of header row

            int itemNumber = 1;
            for (InvoiceItemForm item : items) {
                float rowHeight = calculateWrappedTextHeight(item.getProductName(), 240, FONT_REGULAR, 9);

                // If the current row (plus the totals section) would go off the page, create a new one
                if (cursorY - rowHeight < BOTTOM_MARGIN + 80) {
                    drawTableBorders(tableTopY, cursorY);
                    startNewPage();
                    tableTopY = PAGE_TOP; // Reset top for border drawing on new page
                    drawTableHeader();
                    cursorY -= 25;
                }

                if (itemNumber % 2 == 0) { // Zebra striping
                    cs.setNonStrokingColor(COLOR_ROW_ALT);
                    cs.addRect(MARGIN, cursorY - rowHeight, USABLE_WIDTH, rowHeight);
                    cs.fill();
                }

                cs.setNonStrokingColor(COLOR_PRIMARY_TEXT);
                cs.setFont(FONT_REGULAR, 9);
                float textY = cursorY - 15;

                drawText(String.valueOf(itemNumber++), cs, MARGIN + 10, textY);
                drawWrappedText(item.getProductName(), cs, FONT_REGULAR, 9, MARGIN + 45, textY, 240);
                drawTextRightAligned(String.valueOf(item.getQuantity()), cs, FONT_REGULAR, 9, MARGIN + 350, textY);
                drawTextRightAligned(CURRENCY_SYMBOL + " " + CURRENCY_FORMAT.format(item.getSellingPrice()), cs, FONT_REGULAR, 9, MARGIN + 430, textY);
                drawTextRightAligned(CURRENCY_SYMBOL + " " + CURRENCY_FORMAT.format(item.getQuantity() * item.getSellingPrice()), cs, FONT_BOLD, 9, MARGIN + USABLE_WIDTH - 5, textY);

                cursorY -= rowHeight;
            }
            drawTableBorders(tableTopY, cursorY);
        }

        private void drawTableHeader() throws IOException {
            cs.setNonStrokingColor(COLOR_TABLE_HEADER_BG);
            cs.addRect(MARGIN, cursorY - 25, USABLE_WIDTH, 25);
            cs.fill();
            cs.setNonStrokingColor(COLOR_SECONDARY_TEXT);
            cs.setFont(FONT_BOLD, 10);

            drawText("S.No", cs, MARGIN + 5, cursorY - 16);
            drawText("Item Description", cs, MARGIN + 40, cursorY - 16);
            drawTextRightAligned("Qty", cs, FONT_BOLD, 10, MARGIN + 350, cursorY - 16);
            drawTextRightAligned("Unit Price", cs, FONT_BOLD, 10, MARGIN + 430, cursorY - 16);
            drawTextRightAligned("Total", cs, FONT_BOLD, 10, MARGIN + USABLE_WIDTH - 5, cursorY - 16);
        }

        private void drawTableBorders(float tableTopY, float tableBottomY) throws IOException {
            cs.setStrokingColor(COLOR_TABLE_BORDER);
            cs.setLineWidth(1f);
            cs.addRect(MARGIN, tableBottomY, USABLE_WIDTH, tableTopY - tableBottomY);
            cs.stroke();
        }

        private void drawTotals() throws IOException {
            String totalLabel = "GRAND TOTAL";
            String totalValue = CURRENCY_SYMBOL + " " + CURRENCY_FORMAT.format(form.getTotalAmount());

            // THE FIX: Position the grand total in a fixed footer area at the bottom of the page.
            float totalY = BOTTOM_MARGIN + 30;

            cs.setFont(FONT_BOLD, 18);
            cs.setNonStrokingColor(COLOR_PRIMARY_TEXT);
            drawTextRightAligned(totalLabel, cs, FONT_BOLD, 18, MARGIN + USABLE_WIDTH - 150, totalY);
            drawTextRightAligned(totalValue, cs, FONT_BOLD, 18, MARGIN + USABLE_WIDTH, totalY);
        }

        private void drawFooter() throws IOException {
            String footerText = "Thank you for your business! | www.increff.com";
            float textWidth = (FONT_ITALIC.getStringWidth(footerText) / 1000f) * 9;
            float centerX = MARGIN + (USABLE_WIDTH / 2) - (textWidth / 2);

            cs.setFont(FONT_ITALIC, 9);
            cs.setNonStrokingColor(COLOR_SECONDARY_TEXT);
            drawText(footerText, cs, centerX, BOTTOM_MARGIN - 10);
        }

        // --- Robust Text Drawing and Wrapping Helpers ---

        private void drawText(String text, PDPageContentStream cs, float x, float y) throws IOException {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
        }

        private void drawTextRightAligned(String text, PDPageContentStream cs, PDFont font, float fontSize, float x, float y) throws IOException {
            float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x - textWidth, y);
            cs.showText(text);
            cs.endText();
        }

        private float calculateWrappedTextHeight(String text, float maxWidth, PDFont font, float fontSize) throws IOException {
            if (text == null) return 25f;
            List<String> lines = wrapText(text, font, fontSize, maxWidth);
            return Math.max(25f, lines.size() * 15f); // Minimum row height of 25
        }

        private void drawWrappedText(String text, PDPageContentStream cs, PDFont font, float fontSize, float x, float y, float maxWidth) throws IOException {
            if (text == null) return;
            List<String> lines = wrapText(text, font, fontSize, maxWidth);
            cs.setFont(font, fontSize);
            for (String line : lines) {
                drawText(line, cs, x, y);
                y -= 15; // Move down for the next line
            }
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                lines.add("");
                return lines;
            }
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width > maxWidth && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
            return lines;
        }
    }
}

