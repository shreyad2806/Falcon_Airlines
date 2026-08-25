package com.falcon.airlines.service;

import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.SeatAllocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Service for generating PDF tickets for Falcon Airlines.
 */
@Slf4j
@Service
public class TicketPdfService {

    private final SeatAllocationRepository seatAllocationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public TicketPdfService(SeatAllocationRepository seatAllocationRepository) {
        this.seatAllocationRepository = seatAllocationRepository;
    }

    /**
     * Generate a PDF ticket for a given ticket.
     * Returns the PDF as a byte array.
     */
    public byte[] generateTicketPdf(Ticket ticket) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                drawTicket(contentStream, page, ticket);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF for ticket: {}", ticket.getTicketNumber(), e);
            throw new BaseException("Failed to generate ticket PDF", HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED");
        }
    }

    /**
     * Draw the ticket content on the PDF page.
     */
    private void drawTicket(PDPageContentStream contentStream, PDPage page, Ticket ticket) throws IOException {
        float margin = 50;
        float yPosition = page.getMediaBox().getHeight() - margin;
        float leftMargin = margin;
        float rightMargin = page.getMediaBox().getWidth() - margin;

        // Fonts
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // Header - Airline Name
        contentStream.setFont(boldFont, 24);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("FALCON AIRLINES");
        contentStream.endText();
        yPosition -= 40;

        // Subtitle
        contentStream.setFont(normalFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Electronic Ticket / Itinerary");
        contentStream.endText();
        yPosition -= 50;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Ticket Number
        contentStream.setFont(boldFont, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Ticket Number: " + ticket.getTicketNumber());
        contentStream.endText();
        yPosition -= 25;

        // Booking Reference
        if (ticket.getBooking() != null && ticket.getBooking().getBookingReference() != null) {
            contentStream.setFont(normalFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Booking Reference: " + ticket.getBooking().getBookingReference());
            contentStream.endText();
            yPosition -= 25;
        }

        // Ticket Status
        contentStream.setFont(normalFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Status: " + formatStatus(ticket.getStatus()));
        contentStream.endText();
        yPosition -= 25;

        // Issued Date
        contentStream.setFont(normalFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Issued: " + formatDate(ticket.getIssuedAt()));
        contentStream.endText();
        yPosition -= 40;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Passenger Information
        contentStream.setFont(boldFont, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("PASSENGER INFORMATION");
        contentStream.endText();
        yPosition -= 25;

        contentStream.setFont(normalFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        String passengerName = formatPassengerName(ticket);
        contentStream.showText("Name: " + passengerName);
        contentStream.endText();
        yPosition -= 40;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Flight Information
        contentStream.setFont(boldFont, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("FLIGHT INFORMATION");
        contentStream.endText();
        yPosition -= 25;

        if (ticket.getFlight() != null) {
            Flight flight = ticket.getFlight();

            // Flight Number
            contentStream.setFont(normalFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Flight: " + flight.getFlightNumber());
            contentStream.endText();
            yPosition -= 20;

            // Origin
            if (flight.getOriginAirport() != null) {
                Airport origin = flight.getOriginAirport();
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("From: " + origin.getIataCode() + " - " + origin.getName() + ", " + origin.getCity());
                contentStream.endText();
                yPosition -= 20;
            }

            // Destination
            if (flight.getDestinationAirport() != null) {
                Airport destination = flight.getDestinationAirport();
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("To: " + destination.getIataCode() + " - " + destination.getName() + ", " + destination.getCity());
                contentStream.endText();
                yPosition -= 20;
            }

            // Departure Time
            if (flight.getScheduledDeparture() != null) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Departure: " + formatDate(flight.getScheduledDeparture()));
                contentStream.endText();
                yPosition -= 20;
            }

            // Arrival Time
            if (flight.getScheduledArrival() != null) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Arrival: " + formatDate(flight.getScheduledArrival()));
                contentStream.endText();
                yPosition -= 20;
            }

            // Terminal
            if (flight.getTerminal() != null) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Terminal: " + flight.getTerminal());
                contentStream.endText();
                yPosition -= 20;
            }

            // Gate
            if (flight.getGate() != null) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, yPosition);
                contentStream.showText("Gate: " + flight.getGate());
                contentStream.endText();
                yPosition -= 20;
            }
        }

        yPosition -= 20;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Seat Information
        contentStream.setFont(boldFont, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("SEAT INFORMATION");
        contentStream.endText();
        yPosition -= 25;

        // Get seat allocation
        Optional<SeatAllocation> seatAllocation = seatAllocationRepository.findByTicketId(ticket.getId());
        if (seatAllocation.isPresent() && seatAllocation.get().getSeat() != null) {
            contentStream.setFont(normalFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Seat: " + seatAllocation.get().getSeat().getSeatNumber());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Class: " + seatAllocation.get().getSeat().getSeatClass());
            contentStream.endText();
            yPosition -= 20;
        } else {
            contentStream.setFont(normalFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, yPosition);
            contentStream.showText("Seat: To be assigned");
            contentStream.endText();
            yPosition -= 20;
        }

        yPosition -= 20;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Fare Information
        contentStream.setFont(boldFont, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("FARE INFORMATION");
        contentStream.endText();
        yPosition -= 25;

        contentStream.setFont(normalFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Fare Basis: " + ticket.getFareBasis());
        contentStream.endText();
        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Fare: " + formatCurrency(ticket.getFare()));
        contentStream.endText();
        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Taxes: " + formatCurrency(ticket.getTaxes()));
        contentStream.endText();
        yPosition -= 20;

        BigDecimal total = ticket.getFare().add(ticket.getTaxes());
        contentStream.setFont(boldFont, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("Total: " + formatCurrency(total));
        contentStream.endText();
        yPosition -= 40;

        // Draw horizontal line
        drawLine(contentStream, leftMargin, yPosition, rightMargin, yPosition);
        yPosition -= 30;

        // Footer
        contentStream.setFont(normalFont, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("This is an electronic ticket. Please present this document along with valid");
        contentStream.endText();
        yPosition -= 15;

        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("identification at the airport check-in counter.");
        contentStream.endText();
        yPosition -= 20;

        contentStream.beginText();
        contentStream.newLineAtOffset(leftMargin, yPosition);
        contentStream.showText("For assistance, contact Falcon Airlines customer service.");
        contentStream.endText();
    }

    /**
     * Draw a horizontal line.
     */
    private void drawLine(PDPageContentStream contentStream, float x1, float y1, float x2, float y2) throws IOException {
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    /**
     * Format passenger name safely.
     */
    private String formatPassengerName(Ticket ticket) {
        if (ticket.getPassenger() == null) {
            return "Unknown Passenger";
        }
        String firstName = ticket.getPassenger().getFirstName() != null ? ticket.getPassenger().getFirstName() : "";
        String lastName = ticket.getPassenger().getLastName() != null ? ticket.getPassenger().getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    /**
     * Format ticket status.
     */
    private String formatStatus(TicketStatus status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return status.toString();
    }

    /**
     * Format date/time.
     */
    private String formatDate(Instant instant) {
        if (instant == null) {
            return "TBD";
        }
        return instant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    /**
     * Format currency.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return "$" + amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
