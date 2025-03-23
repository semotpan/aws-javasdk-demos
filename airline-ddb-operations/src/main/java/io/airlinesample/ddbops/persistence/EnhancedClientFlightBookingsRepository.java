package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.FlightBookings;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure.ALL_OLD;

public final class EnhancedClientFlightBookingsRepository implements FlightBookings {

    private final DynamoDbEnhancedClient enhancedClient;

    private final DynamoDbTable<Flight> flightTable;
    private final DynamoDbTable<Booking> bookingTable;

    public EnhancedClientFlightBookingsRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = requireNonNull(enhancedClient);
        this.flightTable = enhancedClient.table(Flight.FLIGHT_TABLE_NAME, TableSchema.fromBean(Flight.class));
        this.bookingTable = enhancedClient.table(Booking.BOOKING_TABLE_NAME, TableSchema.fromBean(Booking.class));
    }

    @Override
    public Optional<Flight> findFlight(FlightPrimaryKey primaryKey) {
        var flight = flightTable.getItem(GetItemEnhancedRequest.builder()
                .key(Key.builder()
                        .partitionValue(primaryKey.getPartitionKey())
                        .sortValue(primaryKey.getSortKey())
                        .build())
                .consistentRead(true)
                .build());

        return Optional.ofNullable(flight);
    }

    @Override
    public Optional<Booking> findBooking(String customerEmail, String bookingID) {
        var booking = bookingTable.getItem(GetItemEnhancedRequest.builder()
                .key(Key.builder()
                        .partitionValue(customerEmail)
                        .sortValue(bookingID)
                        .build())
                .consistentRead(true)
                .build());

        return Optional.ofNullable(booking);
    }

    @Override
    public TransactSummary transactBookFlight(Booking booking, Flight flight) {
        var flightUpdateRequest = TransactUpdateItemEnhancedRequest.builder(Flight.class)
                .item(flight)
                .returnValuesOnConditionCheckFailure(ALL_OLD)
                .build();

        try {
            var writeRequest = TransactWriteItemsEnhancedRequest.builder()
                    .addPutItem(bookingTable, booking)                // Add booking creation to the transaction
                    .addUpdateItem(flightTable, flightUpdateRequest)  // Add flight update to the transaction
                    .build();

            enhancedClient.transactWriteItems(writeRequest);
            return new TransactionSummaryResolver().dynamoTransactSummary();
        } catch (TransactionCanceledException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        } catch (DynamoDbException e) {
            return new TransactionSummaryResolver(e).dynamoTransactSummary();
        }
    }
}
