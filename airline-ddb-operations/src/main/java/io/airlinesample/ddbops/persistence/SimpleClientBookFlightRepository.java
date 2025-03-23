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

@RequiredArgsConstructor
public final class SimpleClientBookFlightRepository implements FlightBookings {

    private final DynamoDbClient dynamoDbClient;

    @Override
    public Optional<Flight> findFlight(FlightPrimaryKey primaryKey) {
        var queryRequest = QueryRequest.builder()
                .tableName(FLIGHT_TABLE_NAME)
                .keyConditionExpression(String.format("%s = :PK AND %s = :SK", Flight.ROUTE_BY_DAY_FIELD_NAME, Flight.DEPARTURE_TIME_FIELD_NAME))
                .expressionAttributeValues(Map.of(
                        ":PK", AttributeValue.fromS(primaryKey.getPartitionKey()),
                        ":SK", AttributeValue.fromS(primaryKey.getSortKey())
                ))
                .consistentRead(true)
                .scanIndexForward(false)  // Improves performance by returning the latest items first
//                .projectionExpression("") // select only needed fields, IMPROVE costs due to smaller amount of data
                .build();

        var queryResponse = dynamoDbClient.query(queryRequest);
        logMultipleItemsWarning(queryResponse, FLIGHT_TABLE_NAME);
        return queryResponse.items().stream()
                .map(FlightMapper::toModel)
                .findFirst();
    }

    @Override
    public Optional<Booking> findBooking(String customerEmail, String bookingID) {
        var queryRequest = QueryRequest.builder()
                .tableName(BOOKING_TABLE_NAME)
                .keyConditionExpression(String.format("%s = :PK AND %s = :SK", CUSTOMER_EMAIL_FIELD_NAME, BOOKING_ID_FIELD_NAME))
                .expressionAttributeValues(Map.of(
                        ":PK", AttributeValue.fromS(customerEmail),
                        ":SK", AttributeValue.fromS(bookingID)
                ))
                .consistentRead(true)
                .scanIndexForward(false)  // Improves performance by returning the latest items first
                .projectionExpression(String.join(",", CUSTOMER_EMAIL_FIELD_NAME, BOOKING_ID_FIELD_NAME, DEPARTURE_DATE_TIME_FIELD_NAME)) // select only needed fields, IMPROVE costs due to smaller amount of data
                .build();

        var queryResponse = dynamoDbClient.query(queryRequest);
        logMultipleItemsWarning(queryResponse, BOOKING_TABLE_NAME);
        return queryResponse.items().stream()
                .map(BookingMapper::toModel)
                .findFirst();
    }

    private void logMultipleItemsWarning(QueryResponse queryResponse, String entity) {
        if (queryResponse.hasItems() && queryResponse.count() > 1) {
            System.err.printf("Warning: More than one %s found, using the first one. Count: %d%n", entity, queryResponse.count());
        }
    }

    @Override
    public TransactSummary transactBookFlight(Booking booking, Flight flight) {
        var transactionExpressions = new BookFlightTransactionExpressions(booking);
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

        var bookingInsertItem = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(BOOKING_TABLE_NAME)
                        .item(BookingMapper.toDDBModel(booking))
                        .build())
                .build();

        var transactWriteItemsRequest = TransactWriteItemsRequest.builder()
                .transactItems(flightUpdateItem, bookingInsertItem)
                .build();

        try {
            dynamoDbClient.transactWriteItems(transactWriteItemsRequest);
            return new TransactionSummaryResolver().dynamoTransactSummary();
        } catch (TransactionCanceledException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        } catch (DynamoDbException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        }
    }

    // Inner class to encapsulate the logic for building flight-related expressions
    private static final class BookFlightTransactionExpressions {

        private final String updateExpression;
        private final String conditionExpression;
        private final Map<String, String> expressionAttributeNames;
        private final Map<String, AttributeValue> expressionAttributeValues;

        private BookFlightTransactionExpressions(Booking booking) {
            if (booking.hasSeatNumber()) {
                this.updateExpression = buildUpdateExpressionWithSeat();
                this.conditionExpression = "TotalSeats > AvailableSeats AND attribute_not_exists(ClaimedSeatMap.#seatNumber)";
                this.expressionAttributeNames = Map.of("#seatNumber", booking.getSeatNumber());
                this.expressionAttributeValues = Map.of(
                        ":one", AttributeValue.fromN("1"),
                        ":bookingID", AttributeValue.fromS(booking.getBookingID())
                );
            } else {
                this.updateExpression = buildUpdateExpressionWithoutSeat();
                this.conditionExpression = "TotalSeats > AvailableSeats";
                this.expressionAttributeNames = null;
                this.expressionAttributeValues = Map.of(":one", AttributeValue.fromN("1"));
            }
        }

        private String buildUpdateExpressionWithSeat() {
            return """
                    SET AvailableSeats = AvailableSeats - :one,
                        Version = Version + :one,
                        ClaimedSeatMap.#seatNumber = :bookingID
                    """;
        }

        private String buildUpdateExpressionWithoutSeat() {
            return """
                    SET AvailableSeats = AvailableSeats - :one,
                        HeldSeats = HeldSeats + :one,
                        Version = Version + :one
                    """;
        }
    }
}
