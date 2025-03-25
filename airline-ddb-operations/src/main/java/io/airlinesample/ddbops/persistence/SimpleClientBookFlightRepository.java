package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.FlightBookings;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

import static io.airlinesample.ddbops.domain.Booking.*;
import static io.airlinesample.ddbops.domain.Flight.FLIGHT_TABLE_NAME;

/**
 * Repository implementation for flight bookings using DynamoDB.
 * <p>
 * This class handles operations such as finding flights, finding bookings,
 * and processing transactional flight bookings. It uses conditional expressions
 * to ensure data consistency and enforce business rules during transactions.
 *
 * <p>
 * The {@code transactBookFlight} method performs a transactional operation
 * <ul>
 *     <li>Decrements available seats for the flight and optionally assigns a specific seat.</li>
 *     <li>Inserts a new booking record into the booking table.</li>
 * </ul>
 * The transaction ensures consistency and applies conditional expressions to validate that there are enough available
 * seats and the booking is valid before committing any changes to the database.
 * </p>
 */
@RequiredArgsConstructor
public final class SimpleClientBookFlightRepository implements FlightBookings {

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<Flight> findFlight(FlightPrimaryKey flightKey) {
        // Build a query request to find a flight by its primary key
        var queryRequest = QueryRequest.builder()
                .tableName(FLIGHT_TABLE_NAME)
                .keyConditionExpression(String.format("%s = :PK AND %s = :SK",
                        Flight.ROUTE_BY_DAY_FIELD_NAME, Flight.DEPARTURE_TIME_FIELD_NAME))
                .expressionAttributeValues(Map.of(
                        ":PK", AttributeValue.fromS(flightKey.getPartitionKey()),
                        ":SK", AttributeValue.fromS(flightKey.getSortKey())
                ))
                .consistentRead(true)
                .scanIndexForward(false)  // Fetch the latest items first
                .projectionExpression(String.join(",",
                        Flight.CLAIMED_SEAT_MAP_FIELD_NAME,
                        Flight.TOTAL_SEATS_FIELD_NAME,
                        Flight.HELD_SEATS_FIELD_NAME,
                        Flight.AVAILABLE_SEATS_FIELD_NAME,
                        Flight.VERSION_FIELD_NAME
                )) // Select only necessary fields to reduce cost and improve performance
                .build();

        // Execute the query
        var queryResponse = dynamoDbClient.query(queryRequest);

        // Log a warning if multiple items are returned
        logWarningIfMultipleItemsFound(queryResponse, FLIGHT_TABLE_NAME);

        // Map the result to a Flight object and return
        return queryResponse.items().stream()
                .map(FlightMapper::toModel)
                .findFirst();
    }

    @Override
    public Optional<Booking> findBooking(String customerEmail, String bookingId) {
        // Build a query request to find a booking by customer email and booking ID
        var queryRequest = QueryRequest.builder()
                .tableName(BOOKING_TABLE_NAME)
                .keyConditionExpression(String.format("%s = :PK AND %s = :SK",
                        CUSTOMER_EMAIL_FIELD_NAME, BOOKING_ID_FIELD_NAME))
                .expressionAttributeValues(Map.of(
                        ":PK", AttributeValue.fromS(customerEmail),
                        ":SK", AttributeValue.fromS(bookingId)
                ))
                .consistentRead(true)
                .scanIndexForward(false)  // Fetch the latest items first
                .projectionExpression(String.join(",",
                        CUSTOMER_EMAIL_FIELD_NAME,
                        BOOKING_ID_FIELD_NAME,
                        DEPARTURE_DATE_TIME_FIELD_NAME)) // Select only necessary fields
                .build();

        // Execute the query
        var queryResponse = dynamoDbClient.query(queryRequest);

        // Log a warning if multiple items are returned
        logWarningIfMultipleItemsFound(queryResponse, BOOKING_TABLE_NAME);

        // Map the result to a Booking object and return
        return queryResponse.items().stream()
                .map(BookingMapper::toModel)
                .findFirst();
    }

    private void logWarningIfMultipleItemsFound(QueryResponse queryResponse, String entityName) {
        // Log a warning if more than one item is found for the given entity
        if (queryResponse.hasItems() && queryResponse.count() > 1) {
            System.err.printf("Warning: Multiple %s records found. Using the first one. Count: %d%n",
                    entityName, queryResponse.count());
        }
    }

    @Override
    public TransactSummary transactBookFlight(Booking booking, Flight flight) {
        // Create transaction expressions for updating flight and inserting booking
        var transactionExpressions = new BookFlightTransactionExpressions(booking, flight);

        // Define the flight update transaction item
        var flightUpdateItem = TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(FLIGHT_TABLE_NAME)
                        .key(FlightMapper.toDDBKeyMap(booking.flightPrimaryKey()))
                        .updateExpression(transactionExpressions.updateExpression)
                        .conditionExpression(transactionExpressions.conditionExpression)
                        .expressionAttributeNames(transactionExpressions.expressionAttributeNames)
                        .expressionAttributeValues(transactionExpressions.expressionAttributeValues)
                        .build())
                .build();

        // Define the booking insertion transaction item
        var bookingInsertItem = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(BOOKING_TABLE_NAME)
                        .item(BookingMapper.toDDBModel(booking))
                        .build())
                .build();

        // Combine transaction items into a transaction request
        var transactionRequest = TransactWriteItemsRequest.builder()
                .transactItems(flightUpdateItem, bookingInsertItem)
                .build();

        // Execute the transaction and handle exceptions
        try {
            dynamoDbClient.transactWriteItems(transactionRequest);
            return new TransactionSummaryResolver().dynamoTransactSummary();
        } catch (TransactionCanceledException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        } catch (DynamoDbException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        }
    }

    // Inner class to handle building transaction expressions
    private static final class BookFlightTransactionExpressions {

        private final String updateExpression;
        private final String conditionExpression;
        private final Map<String, String> expressionAttributeNames;
        private final Map<String, AttributeValue> expressionAttributeValues;

        private BookFlightTransactionExpressions(Booking booking, Flight flight) {
            // Optimistic locking condition to ensure version consistency
            this.conditionExpression = "Version = :expectedVersion";

            // Update expressions and attributes differ based on whether the booking has a specific seat
            if (booking.hasSeatNumber()) {
                this.updateExpression = buildUpdateExpressionWithSpecificSeat();
                this.expressionAttributeNames = Map.of("#seatNumber", booking.getSeatNumber());
                this.expressionAttributeValues = Map.of(
                        ":seatDecrement", AttributeValue.fromN("1"),
                        ":bookingId", AttributeValue.fromS(booking.getBookingID()),
                        ":expectedVersion", AttributeValue.fromN(flight.getVersion().toString())
                );
            } else {
                this.updateExpression = buildUpdateExpressionWithoutSpecificSeat();
                this.expressionAttributeNames = null;
                this.expressionAttributeValues = Map.of(
                        ":seatDecrement", AttributeValue.fromN("1"),
                        ":expectedVersion", AttributeValue.fromN(flight.getVersion().toString())
                );
            }
        }

        private String buildUpdateExpressionWithSpecificSeat() {
            // Build an update expression for cases where a specific seat is booked
            return """
                    SET AvailableSeats = AvailableSeats - :seatDecrement,
                        Version = Version + :seatDecrement,
                        ClaimedSeatMap.#seatNumber = :bookingId
                    """;
        }

        private String buildUpdateExpressionWithoutSpecificSeat() {
            // Build an update expression for cases where no specific seat is booked
            return """
                    SET AvailableSeats = AvailableSeats - :seatDecrement,
                        HeldSeats = HeldSeats + :seatDecrement,
                        Version = Version + :seatDecrement
                    """;
        }
    }
}
