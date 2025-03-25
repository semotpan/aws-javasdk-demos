package io.airlinesample.ddbops.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@DynamoDbBean
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Flight {

    public static final String FLIGHT_TABLE_NAME = "flights";

    public static final String ROUTE_BY_DAY_FIELD_NAME = "RouteByDay";
    public static final String DEPARTURE_TIME_FIELD_NAME = "DepartureTime";
    public static final String FLIGHT_NUMBER_FIELD_NAME = "FlightNumber";
    public static final String AIRPLANE_MODEL_FIELD_NAME = "AirplaneModel";
    public static final String TOTAL_SEATS_FIELD_NAME = "TotalSeats";
    public static final String AVAILABLE_SEATS_FIELD_NAME = "AvailableSeats";
    public static final String HELD_SEATS_FIELD_NAME = "HeldSeats";
    public static final String VERSION_FIELD_NAME = "Version";
    public static final String CLAIMED_SEAT_MAP_FIELD_NAME = "ClaimedSeatMap";

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(ROUTE_BY_DAY_FIELD_NAME)
    }))
    @JsonProperty(ROUTE_BY_DAY_FIELD_NAME)
    private String routeByDay;

    @Getter(onMethod = @__({
            @DynamoDbSortKey,
            @DynamoDbAttribute(DEPARTURE_TIME_FIELD_NAME)
    }))
    @JsonProperty(DEPARTURE_TIME_FIELD_NAME)
    private String departureTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(FLIGHT_NUMBER_FIELD_NAME)}))
    @JsonProperty(FLIGHT_NUMBER_FIELD_NAME)
    private String flightNumber;

    @Getter(onMethod = @__({@DynamoDbAttribute(AIRPLANE_MODEL_FIELD_NAME)}))
    @JsonProperty(AIRPLANE_MODEL_FIELD_NAME)
    private String airplaneModel;

    @Getter(onMethod = @__({@DynamoDbAttribute(TOTAL_SEATS_FIELD_NAME)}))
    @JsonProperty(TOTAL_SEATS_FIELD_NAME)
    private Integer totalSeats;

    @Getter(onMethod = @__({@DynamoDbAttribute(AVAILABLE_SEATS_FIELD_NAME)}))
    @JsonProperty(AVAILABLE_SEATS_FIELD_NAME)
    private Integer availableSeats;

    @Getter(onMethod = @__({@DynamoDbAttribute(HELD_SEATS_FIELD_NAME)}))
    @JsonProperty(HELD_SEATS_FIELD_NAME)
    private Integer heldSeats;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(VERSION_FIELD_NAME),
            @DynamoDbVersionAttribute
    }))
    @JsonProperty(VERSION_FIELD_NAME)
    private Long version;

    @Getter(onMethod = @__({@DynamoDbAttribute(CLAIMED_SEAT_MAP_FIELD_NAME)}))
    @JsonProperty(CLAIMED_SEAT_MAP_FIELD_NAME)
    private Map<String, String> claimedSeatMap = new HashMap<>();

    @Builder(builderClassName = "MapFlightBuilder", builderMethodName = "mapBuilder")
    // CREATE NEW routes, or build exiting
    public Flight(String routeByDay, String departureTime, String flightNumber, String airplaneModel,
                  Integer totalSeats, Integer availableSeats, Integer heldSeats,
                  Long version, Map<String, String> claimedSeatMap) {

        this.routeByDay = routeByDay;
        this.departureTime = departureTime;
        this.flightNumber = flightNumber;
        this.airplaneModel = airplaneModel;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.version = version;
        this.heldSeats = heldSeats;

        this.claimedSeatMap = isNull(claimedSeatMap) ? new HashMap<>() : claimedSeatMap;
    }

    @Builder(builderClassName = "NewFlightBuilder")
    // CREATE NEW routes
    public Flight(FlightPrimaryKey primaryKey, String flightNumber, String airplaneModel, Integer totalSeats) {
        requireNonNull(primaryKey, "primaryKey cannot be null");

        this.routeByDay = primaryKey.getPartitionKey();
        this.departureTime = primaryKey.getSortKey();

        this.flightNumber = requireNonNull(flightNumber, "flightNumber cannot be null");
        this.airplaneModel = requireNonNull(airplaneModel, "airplaneModel cannot be null");

        if (isNull(totalSeats) || totalSeats < 1) {
            throw new IllegalArgumentException("totalSeats must be greater than 0");
        }

        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.heldSeats = 0;

        this.claimedSeatMap = new HashMap<>();
    }

    public void decrementAvailableSeats() {
        this.availableSeats--;
    }

    public boolean addSeatIfAvailable(String seatNumber, String bookingId) {
        if (this.claimedSeatMap == null) {
            this.claimedSeatMap = new HashMap<>();
        }

        if (!claimedSeatMap.containsKey(seatNumber)) {
            claimedSeatMap.put(seatNumber, bookingId);
            return true;
        }

        return false;
    }

    public boolean anySeatAvailable() {
        return this.availableSeats > 0;
    }

    public void incrementHeldSeats() {
        this.heldSeats++;
    }
}
