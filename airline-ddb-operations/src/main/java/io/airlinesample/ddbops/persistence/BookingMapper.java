package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Booking;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static io.airlinesample.ddbops.domain.Booking.*;

final class BookingMapper {

    static Map<String, AttributeValue> toDDBModel(final Booking booking) {
        var bookingAttributes = new HashMap<String, AttributeValue>();
        bookingAttributes.put(CUSTOMER_EMAIL_FIELD_NAME, AttributeValue.builder().s(booking.getCustomerEmail()).build());
        bookingAttributes.put(BOOKING_ID_FIELD_NAME, AttributeValue.builder().s(booking.getBookingID()).build());
        bookingAttributes.put(FLIGHT_NUMBER_FIELD_NAME, AttributeValue.builder().s(booking.getFlightNumber()).build());
        bookingAttributes.put(SOURCE_FIELD_NAME, AttributeValue.builder().s(booking.getSource()).build());
        bookingAttributes.put(DESTINATION_FIELD_NAME, AttributeValue.builder().s(booking.getDestination()).build());
        bookingAttributes.put(DEPARTURE_DATE_TIME_FIELD_NAME, AttributeValue.builder().n(booking.getDepartureDateTime().toString()).build());
        bookingAttributes.put(FARE_CLASS_FIELD_NAME, AttributeValue.builder().s(booking.getFareClass()).build());

        if (booking.hasSeatNumber()) {
            bookingAttributes.put(SEAT_NUMBER_FIELD_NAME, AttributeValue.builder().s(booking.getSeatNumber()).build());
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
        if (row.containsKey(fieldName)) {
            return row.get(fieldName).s();
        }

        return null;
    }

    private static Long safeLongRead(Map<String, AttributeValue> row, String fieldName) {
        if (row.containsKey(fieldName)) {
            return Long.parseLong(row.get(fieldName).n());
        }

        return null;
    }
}
