package com.falcon.airlines.service;

import com.falcon.airlines.entity.BoardingPass;
import com.falcon.airlines.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating boarding-pass PDFs for Falcon Airlines.
 *
 * The PDF contains all boarding-pass fields and a scannable QR code
 * derived from the boarding pass's verification token.
 */
@Slf4j
@Service
public class BoardingPassPdfService {

    private final QrCodeService qrCodeService;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public BoardingPassPdfService(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Generate a boarding-pass PDF for the given entity.
     *
     * @param boardingPass the boarding-pass entity (must be authorised for the caller)
     * @return PDF bytes ready to stream as APPLICATION_PDF
     */
    public byte[] generateBoardingPassPdf(BoardingPass boardingPass) {
        try (PDDocument document = new PDDocument()) {
            // Use a landscape-oriented page that matches a standard boarding pass ratio
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawBoardingPass(cs, page, boardingPass);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate boarding-pass PDF for {}",
                    boardingPass.getBoardingPassNumber(), e);
            throw new BaseException("Failed to generate boarding-pass PDF",
                    HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED");
        }
    }

    // ------------------------------------------------------------------ drawing

    private void drawBoardingPass(PDPageContentStream cs, PDPage page,
                                  BoardingPass bp) throws IOException {

        float pageW = page.getMediaBox().getWidth();
        float pageH = page.getMediaBox().getHeight();
        float margin = 20f;

        // Fonts (Standard14 only supports Latin-1 / ASCII)
        PDType1Font boldLg   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font boldMd   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font normalMd = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font normalSm = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // colours (RGB)
        float[] primary   = {0.0f, 0.15f, 0.35f};   // dark navy
        float[] accent    = {0.0f, 0.40f, 0.65f};    // teal accent
        float[] separator = {0.75f, 0.78f, 0.82f};   // medium grey

        // Layout constants
        float leftX  = margin;
        float rightX = pageW - margin;
        float contentW = rightX - leftX;

        // The boarding pass is split: 62 % main info (left) | 38 % stub (right)
        float splitX = leftX + contentW * 0.62f;

        // =================================================================
        //  BACKGROUND
        // =================================================================
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.addRect(0, 0, pageW, pageH);
        cs.fill();

        // =================================================================
        //  HEADER BANNER
        // =================================================================
        float bannerH = 50f;
        float bannerY = pageH - margin - bannerH;
        cs.setNonStrokingColor(primary[0], primary[1], primary[2]);
        cs.addRect(margin, bannerY, contentW, bannerH);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldLg, 20);
        cs.beginText();
        cs.newLineAtOffset(leftX + 10, bannerY + 15);
        cs.showText("FALCON AIRLINES");
        cs.endText();

        cs.setFont(normalMd, 11);
        cs.beginText();
        cs.newLineAtOffset(leftX + 10, bannerY + 2);
        cs.showText("BOARDING PASS");
        cs.endText();

        // Status badge (right side of banner)
        String statusText = bp.getStatus() != null ? bp.getStatus().toString() : "UNKNOWN";
        cs.setFont(boldMd, 11);
        float statusW = boldMd.getStringWidth(statusText) / 1000f * 11;
        cs.beginText();
        cs.newLineAtOffset(splitX - statusW - 10, bannerY + 18);
        cs.showText(statusText);
        cs.endText();

        // =================================================================
        //  VERTICAL DOTTED SEPARATOR (left / stub)
        // =================================================================
        drawDottedLine(cs, splitX, bannerY, splitX, margin, separator);

        // =================================================================
        //  MAIN SECTION (left)
        // =================================================================
        float y = bannerY - 20;

        // Passenger name
        y = drawLabelValue(cs, leftX, y, "PASSENGER", safeName(bp), boldMd, normalMd, accent);

        // Route: ORIGIN -> DESTINATION (large)
        y -= 8;
        String originCode = safeCode(bp.getFlight() != null && bp.getFlight().getOriginAirport() != null
                ? bp.getFlight().getOriginAirport().getIataCode() : null);
        String destCode = safeCode(bp.getFlight() != null && bp.getFlight().getDestinationAirport() != null
                ? bp.getFlight().getDestinationAirport().getIataCode() : null);

        cs.setNonStrokingColor(primary[0], primary[1], primary[2]);
        cs.setFont(boldLg, 28);
        cs.beginText();
        cs.newLineAtOffset(leftX, y - 20);
        cs.showText(originCode);
        cs.endText();

        // Arrow
        cs.setFont(normalMd, 18);
        float originW = boldLg.getStringWidth(originCode) / 1000f * 28;
        cs.setNonStrokingColor(accent[0], accent[1], accent[2]);
        cs.beginText();
        cs.newLineAtOffset(leftX + originW + 12, y - 18);
        cs.showText("->");
        cs.endText();

        // Destination
        cs.setNonStrokingColor(primary[0], primary[1], primary[2]);
        cs.setFont(boldLg, 28);
        float arrowW = normalMd.getStringWidth("->") / 1000f * 18;
        cs.beginText();
        cs.newLineAtOffset(leftX + originW + 12 + arrowW + 12, y - 20);
        cs.showText(destCode);
        cs.endText();

        y -= 38;

        // City names
        String originCity = safe(bp.getFlight() != null && bp.getFlight().getOriginAirport() != null
                ? bp.getFlight().getOriginAirport().getCity() : "");
        String destCity = safe(bp.getFlight() != null && bp.getFlight().getDestinationAirport() != null
                ? bp.getFlight().getDestinationAirport().getCity() : "");
        cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        cs.setFont(normalSm, 9);
        cs.beginText();
        cs.newLineAtOffset(leftX, y);
        cs.showText(originCity + (destCity.isEmpty() ? "" : "  ->  " + destCity));
        cs.endText();

        y -= 16;

        // Thin separator
        drawLine(cs, leftX, y, splitX - 10, y, separator);
        y -= 14;

        // Info grid: two columns
        float colW = (splitX - leftX - 20) / 2;
        float col1 = leftX;
        float col2 = leftX + colW + 10;

        // Row 1: Flight | Date
        y = drawLabelValue(cs, col1, y, "FLIGHT", safe(bp.getFlight() != null
                ? bp.getFlight().getFlightNumber() : null), boldMd, normalMd, accent);
        drawLabelValue(cs, col2, y + 0, "DATE", formatDate(bp.getFlight() != null
                ? bp.getFlight().getScheduledDeparture() : null), boldMd, normalMd, accent);
        y -= 18;

        // Row 2: Departure | Gate
        y = drawLabelValue(cs, col1, y, "DEPARTURE", formatTime(bp.getFlight() != null
                ? bp.getFlight().getScheduledDeparture() : null), boldMd, normalMd, accent);
        drawLabelValue(cs, col2, y + 0, "GATE", safe(bp.getGate()), boldMd, normalMd, accent);
        y -= 18;

        // Row 3: Boarding Time | Seat
        y = drawLabelValue(cs, col1, y, "BOARDING", formatTime(bp.getBoardingTime()), boldMd, normalMd, accent);
        drawLabelValue(cs, col2, y + 0, "SEAT", safe(bp.getSeatNumber()), boldMd, normalMd, accent);
        y -= 18;

        // Row 4: Class | Boarding Group
        y = drawLabelValue(cs, col1, y, "CLASS", safe(bp.getSeatClass()), boldMd, normalMd, accent);
        drawLabelValue(cs, col2, y + 0, "GROUP", safe(bp.getBoardingGroup()), boldMd, normalMd, accent);
        y -= 18;

        // Row 5: Booking Ref | Ticket No
        y = drawLabelValue(cs, col1, y, "BOOKING REF", safe(bp.getBooking() != null
                ? bp.getBooking().getBookingReference() : null), boldMd, normalMd, accent);
        drawLabelValue(cs, col2, y + 0, "TICKET NO", safe(bp.getTicket() != null
                ? bp.getTicket().getTicketNumber() : null), boldMd, normalMd, accent);
        y -= 18;

        // =================================================================
        //  STUB / QR SECTION (right)
        // =================================================================
        float stubLeft = splitX + 12;
        float stubW = rightX - stubLeft;
        float stubY = bannerY - 20;

        // Passenger name on stub
        cs.setNonStrokingColor(primary[0], primary[1], primary[2]);
        cs.setFont(boldMd, 10);
        cs.beginText();
        cs.newLineAtOffset(stubLeft, stubY);
        cs.showText(safeName(bp));
        cs.endText();
        stubY -= 14;

        // Flight + Seat on stub
        cs.setFont(normalSm, 9);
        cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
        String flightSeatLine = safe(bp.getFlight() != null ? bp.getFlight().getFlightNumber() : null)
                + "  |  " + safe(bp.getSeatNumber());
        cs.beginText();
        cs.newLineAtOffset(stubLeft, stubY);
        cs.showText(flightSeatLine);
        cs.endText();
        stubY -= 14;

        // Route on stub
        cs.setFont(boldMd, 12);
        cs.setNonStrokingColor(primary[0], primary[1], primary[2]);
        cs.beginText();
        cs.newLineAtOffset(stubLeft, stubY);
        cs.showText(originCode + " -> " + destCode);
        cs.endText();
        stubY -= 20;

        // QR Code
        try {
            float qrSize = Math.min(stubW, 140f);
            // Generate QR from the verification token
            byte[] qrPng = qrCodeService.generateQrCodeBytes(bp.getVerificationToken());
            PDImageXObject qrImage = PDImageXObject.createFromByteArray(document(), qrPng, "QR");
            float qrX = stubLeft + (stubW - qrSize) / 2;
            cs.drawImage(qrImage, qrX, stubY - qrSize, qrSize, qrSize);
            stubY -= qrSize + 8;
        } catch (Exception e) {
            log.warn("QR code generation failed for boarding pass {}, embedding placeholder",
                    bp.getBoardingPassNumber(), e);
            cs.setFont(normalSm, 8);
            cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
            cs.beginText();
            cs.newLineAtOffset(stubLeft, stubY - 40);
            cs.showText("[QR unavailable]");
            cs.endText();
            stubY -= 50;
        }

        // Boarding pass number on stub (below QR)
        cs.setFont(normalSm, 8);
        cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        cs.beginText();
        cs.newLineAtOffset(stubLeft, stubY);
        cs.showText("BP: " + safe(bp.getBoardingPassNumber()));
        cs.endText();

        // =================================================================
        //  FOOTER
        // =================================================================
        float footerY = margin + 6;
        drawLine(cs, leftX, footerY + 14, rightX, footerY + 14, separator);
        cs.setFont(normalSm, 7);
        cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
        cs.beginText();
        cs.newLineAtOffset(leftX, footerY);
        cs.showText("Please present this boarding pass along with a valid photo ID at the gate.");
        cs.endText();
        cs.beginText();
        cs.newLineAtOffset(rightX - 180, footerY);
        cs.showText("Falcon Airlines  |  falconairlines.com");
        cs.endText();
    }

    // ================================================================ helpers

    /** Create a throw-away PDDocument solely for PDImageXObject.createFromByteArray. */
    private static PDDocument document() {
        return new PDDocument();
    }

    /** Draw a label + value pair, return the new y position. */
    private float drawLabelValue(PDPageContentStream cs, float x, float y,
                                 String label, String value,
                                 PDType1Font labelFont, PDType1Font valueFont,
                                 float[] accent) throws IOException {
        // Label
        cs.setNonStrokingColor(0.45f, 0.48f, 0.52f);
        cs.setFont(labelFont, 7);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(label.toUpperCase());
        cs.endText();

        // Value
        cs.setNonStrokingColor(0.12f, 0.12f, 0.12f);
        cs.setFont(valueFont, 12);
        cs.beginText();
        cs.newLineAtOffset(x, y - 12);
        cs.showText(value);
        cs.endText();

        return y - 26;
    }

    private static void drawLine(PDPageContentStream cs, float x1, float y1,
                                 float x2, float y2, float[] color) throws IOException {
        cs.setStrokingColor(color[0], color[1], color[2]);
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void drawDottedLine(PDPageContentStream cs, float x1, float y1,
                                       float x2, float y2, float[] color) throws IOException {
        cs.setStrokingColor(color[0], color[1], color[2]);
        cs.setLineWidth(0.4f);
        float dashLength = 3f;
        float gapLength = 4f;
        float totalLength = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        float dx = (x2 - x1) / totalLength;
        float dy = (y2 - y1) / totalLength;

        float pos = 0;
        while (pos < totalLength) {
            float startX = x1 + dx * pos;
            float startY = y1 + dy * pos;
            float endPos = Math.min(pos + dashLength, totalLength);
            float endX = x1 + dx * endPos;
            float endY = y1 + dy * endPos;
            cs.moveTo(startX, startY);
            cs.lineTo(endX, endY);
            cs.stroke();
            pos += dashLength + gapLength;
        }
    }

    private static String safeName(BoardingPass bp) {
        if (bp.getPassenger() != null) {
            String fn = bp.getPassenger().getFirstName() != null ? bp.getPassenger().getFirstName() : "";
            String ln = bp.getPassenger().getLastName() != null ? bp.getPassenger().getLastName() : "";
            String name = (fn + " " + ln).trim();
            if (!name.isEmpty()) return name;
        }
        return "N/A";
    }

    /** Airport IATA codes are always ASCII, but guard against null/empty. */
    private static String safeCode(String value) {
        return (value == null || value.isBlank()) ? "---" : value;
    }

    private static String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "TBD";
        return instant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "TBD";
        return instant.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
    }
}
