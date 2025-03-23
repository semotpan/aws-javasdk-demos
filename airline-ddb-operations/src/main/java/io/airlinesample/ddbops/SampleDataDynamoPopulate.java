package io.airlinesample.ddbops;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.Passenger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class SampleDataDynamoPopulate {

    public static void main(String[] args) {
        var sampleDataDynamoPopulate = new SampleDataDynamoPopulate();

        if (sampleDataDynamoPopulate.populated) {
            System.out.println("Data dynamo db populated!");
        }
    }

    public final boolean populated;

    public SampleDataDynamoPopulate() {
        var success = false;
        try (var dynamoDbClient = AwsClientProvider.dynamoDbClient()) {
            var enhancedClient = AwsClientProvider.dynamoDbEnhancedClient(dynamoDbClient);

            var passengerDynamoDbTable = enhancedClient.table(Passenger.PASSENGER_TABLE_NAME, TableSchema.fromBean(Passenger.class));
            var flightDynamoDbTable = enhancedClient.table(Flight.FLIGHT_TABLE_NAME, TableSchema.fromBean(Flight.class));
            var bookingDynamoDbTable = enhancedClient.table(Booking.BOOKING_TABLE_NAME, TableSchema.fromBean(Booking.class));

            InMemoryData.passengers()
                    .forEach(passengerDynamoDbTable::putItem);

            InMemoryData.flights()
                    .forEach(flightDynamoDbTable::putItem);

            InMemoryData.bookings()
                    .forEach(bookingDynamoDbTable::putItem);

            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.populated = success;
    }
}
