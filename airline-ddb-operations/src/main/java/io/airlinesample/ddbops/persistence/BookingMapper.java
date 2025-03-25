package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Booking;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.airlinesample.ddbops.domain.Booking.*;

final class BookingMapper {

    static Map<String, AttributeValue> toDDBModel(final Booking booking) {
        var bookingAttributes = new HashMap<String, AttributeValue>();

        // Populate required attributes
        bookingAttributes.put(CUSTOMER_EMAIL_FIELD_NAME, AttributeValue.fromS(booking.getCustomerEmail()));
        bookingAttributes.put(BOOKING_ID_FIELD_NAME, AttributeValue.fromS(booking.getBookingID()));
        bookingAttributes.put(FLIGHT_NUMBER_FIELD_NAME, AttributeValue.fromS(booking.getFlightNumber()));
        bookingAttributes.put(SOURCE_FIELD_NAME, AttributeValue.fromS(booking.getSource()));
        bookingAttributes.put(DESTINATION_FIELD_NAME, AttributeValue.fromS(booking.getDestination()));
        bookingAttributes.put(DEPARTURE_DATE_TIME_FIELD_NAME, AttributeValue.fromN(booking.getDepartureDateTime().toString()));
        bookingAttributes.put(FARE_CLASS_FIELD_NAME, AttributeValue.fromS(booking.getFareClass()));

        // Optionally add seat number if present
        if (booking.hasSeatNumber()) {
            bookingAttributes.put(SEAT_NUMBER_FIELD_NAME, AttributeValue.fromS(booking.getSeatNumber()));
        }

        return bookingAttributes;
    }

    static Booking toModel(Map<String, AttributeValue> row) {
        return Booking.builder()
                .customerEmail(safeStringRead(row, CUSTOMER_EMAIL_FIELD_NAME))
                .bookingID(safeStringRead(row, BOOKING_ID_FIELD_NAME))
                .flightNumber(safeStringRead(row, FLIGHT_NUMBER_FIELD_NAME))
                .source(safeStringRead(row, SOURCE_FIELD_NAME))
                .destination(safeStringRead(row, DESTINATION_FIELD_NAME))
                .fareClass(safeStringRead(row, FARE_CLASS_FIELD_NAME))
                .seatNumber(safeStringRead(row, SEAT_NUMBER_FIELD_NAME))
                .departureDateTime(safeLongRead(row, DEPARTURE_DATE_TIME_FIELD_NAME))
                .build();
    }

    private static String safeStringRead(Map<String, AttributeValue> row, String fieldName) {
        return Optional.ofNullable(row.get(fieldName))
                .map(AttributeValue::s)
                .orElse(null);
    }

    private static Long safeLongRead(Map<String, AttributeValue> row, String fieldName) {
        return Optional.ofNullable(row.get(fieldName))
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .orElse(null);
    }
}
