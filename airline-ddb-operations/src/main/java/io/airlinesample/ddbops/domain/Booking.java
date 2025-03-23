package io.airlinesample.ddbops.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.time.ZoneOffset;

@DynamoDbBean
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Booking {

    public static final String BOOKING_TABLE_NAME = "bookings";

    public static final String CUSTOMER_EMAIL_FIELD_NAME = "CustomerEmail";
    public static final String BOOKING_ID_FIELD_NAME = "BookingID";
    public static final String FLIGHT_NUMBER_FIELD_NAME = "FlightNumber";
    public static final String SOURCE_FIELD_NAME = "Source";
    public static final String DESTINATION_FIELD_NAME = "Destination";
    public static final String DEPARTURE_DATE_TIME_FIELD_NAME = "DepartureDateTime";
    public static final String SEAT_NUMBER_FIELD_NAME = "SeatNumber";
    public static final String FARE_CLASS_FIELD_NAME = "FareClass";

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(CUSTOMER_EMAIL_FIELD_NAME)
    }))
    @JsonProperty(CUSTOMER_EMAIL_FIELD_NAME)
    private String customerEmail;

    @Getter(onMethod = @__({
            @DynamoDbSortKey,
            @DynamoDbAttribute(BOOKING_ID_FIELD_NAME)
    }))
    @JsonProperty(BOOKING_ID_FIELD_NAME)
    private String bookingID;

    @Getter(onMethod = @__({@DynamoDbAttribute(FLIGHT_NUMBER_FIELD_NAME)}))
    @JsonProperty(FLIGHT_NUMBER_FIELD_NAME)
    private String flightNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(SOURCE_FIELD_NAME)}))
    @JsonProperty(SOURCE_FIELD_NAME)
    private String source;

    @Getter(onMethod = @__({@DynamoDbAttribute(DESTINATION_FIELD_NAME)}))
    @JsonProperty(DESTINATION_FIELD_NAME)
    private String destination;

    @Getter(onMethod = @__({@DynamoDbAttribute(DEPARTURE_DATE_TIME_FIELD_NAME)}))
    @JsonProperty(DEPARTURE_DATE_TIME_FIELD_NAME)
    private Long departureDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(SEAT_NUMBER_FIELD_NAME)}))
    @JsonProperty(SEAT_NUMBER_FIELD_NAME)
    private String seatNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(FARE_CLASS_FIELD_NAME)}))
    @JsonProperty(FARE_CLASS_FIELD_NAME)
    private String fareClass;

    @Builder
    public Booking(String customerEmail, String bookingID, String flightNumber, String source, String destination,
                   Long departureDateTime, String seatNumber, String fareClass) {
        this.customerEmail = customerEmail;
        this.bookingID = bookingID;
        this.flightNumber = flightNumber;
        this.source = source;
        this.destination = destination;
        this.departureDateTime = departureDateTime;
        this.seatNumber = seatNumber;
        this.fareClass = fareClass;
    }

    public FlightPrimaryKey flightPrimaryKey() {
        return FlightPrimaryKey.builder()
                .sourceAirportCode(this.source)
                .destinationAirportCode(this.destination)
                .departureDateTime(Instant.ofEpochSecond(departureDateTime)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime())
                .build();
    }

    public boolean hasSeatNumber() {
        return this.seatNumber != null;
    }
}
