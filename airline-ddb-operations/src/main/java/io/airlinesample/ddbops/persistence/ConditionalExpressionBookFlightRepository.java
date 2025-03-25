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

import static io.airlinesample.ddbops.domain.Booking.BOOKING_TABLE_NAME;
import static io.airlinesample.ddbops.domain.Flight.FLIGHT_TABLE_NAME;

/**
 * Repository implementation for flight bookings using DynamoDB.
 * <p>
 * This class handles operations such as finding flights, finding bookings,
 * and processing transactional flight bookings. It uses conditional expressions
 * to ensure data consistency and enforce business rules during transactions.
 * <p>
 * The {@code transactBookFlight} method performs a transactional operation
 * that:
 * <ul>
 *     <li>Decrements the available seats on a flight record and optionally assigns a seat number.</li>
 *     <li>Inserts a new booking record into the database.</li>
 * </ul>
 * The transaction ensures that the operations are performed atomically and
 * consistent, using conditional expressions to validate that there are enough
 * available seats before making any changes.
 */
@RequiredArgsConstructor
public final class ConditionalExpressionBookFlightRepository implements FlightBookings {

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<Flight> findFlight(FlightPrimaryKey flightKey) {
        throw new UnsupportedOperationException("findFlight is not implemented yet");
    }

    @Override
    public Optional<Booking> findBooking(String customerEmail, String bookingId) {
        throw new UnsupportedOperationException("findBooking is not implemented yet");
    }

    @Override
    public TransactSummary transactBookFlight(Booking booking, Flight flight) {
        // Create transaction expressions for conditional updates
        var transactionExpressions = new FlightBookingTransactionExpressions(booking);

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

    /**
     * Inner class for encapsulating the logic to build conditional expressions
     * and update expressions for flight booking operations.
     */
    private static final class FlightBookingTransactionExpressions {

        private final String updateExpression;
        private final String conditionExpression;
        private final Map<String, String> expressionAttributeNames;
        private final Map<String, AttributeValue> expressionAttributeValues;

        private FlightBookingTransactionExpressions(Booking booking) {
            if (booking.hasSeatNumber()) {
                // Build expressions for bookings with a specific seat number
                this.updateExpression = buildUpdateExpressionWithSeat();
                this.conditionExpression = """
                        AvailableSeats > :noAvailableSeats AND attribute_not_exists(ClaimedSeatMap.#seatNumber)
                        """;
                this.expressionAttributeNames = Map.of("#seatNumber", booking.getSeatNumber());
                this.expressionAttributeValues = Map.of(
                        ":one", AttributeValue.fromN("1"),
                        ":bookingId", AttributeValue.fromS(booking.getBookingID()),
                        ":noAvailableSeats", AttributeValue.fromN("0")
                );
            } else {
                // Build expressions for bookings without a specific seat number
                this.updateExpression = buildUpdateExpressionWithoutSeat();
                this.conditionExpression = "AvailableSeats > :noAvailableSeats";
                this.expressionAttributeNames = null;
                this.expressionAttributeValues = Map.of(
                        ":one", AttributeValue.fromN("1"),
                        ":noAvailableSeats", AttributeValue.fromN("0")
                );
            }
        }

        /**
         * Builds the update expression for bookings with a specific seat number.
         */
        private String buildUpdateExpressionWithSeat() {
            return """
                    SET AvailableSeats = AvailableSeats - :one,
                        Version = Version + :one,
                        ClaimedSeatMap.#seatNumber = :bookingId
                    """;
        }

        /**
         * Builds the update expression for bookings without a specific seat number, increment HeldSeats.
         */
        private String buildUpdateExpressionWithoutSeat() {
            return """
                    SET AvailableSeats = AvailableSeats - :one,
                        HeldSeats = HeldSeats + :one,
                        Version = Version + :one
                    """;
        }
    }
}
