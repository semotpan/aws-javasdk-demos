package io.airlinesample.ddbops;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.Passenger;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AirlineDynamoDbDataInitializer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        var dataInitializer = new AirlineDynamoDbDataInitializer();

        if (dataInitializer.success) {
            System.out.println("✅ Airline data inserted into DynamoDB successfully!");
        }
    }

    public final boolean success;

    public AirlineDynamoDbDataInitializer() {
        this.success = initializeData();
    }

    private boolean initializeData() {
        try (var dynamoDbClient = AwsClientProvider.dynamoDbClient()) {
            var enhancedClient = AwsClientProvider.dynamoDbEnhancedClient(dynamoDbClient);

            var passengerTable = enhancedClient.table(Passenger.PASSENGER_TABLE_NAME, TableSchema.fromBean(Passenger.class));
            var flightTable = enhancedClient.table(Flight.FLIGHT_TABLE_NAME, TableSchema.fromBean(Flight.class));
            var bookingTable = enhancedClient.table(Booking.BOOKING_TABLE_NAME, TableSchema.fromBean(Booking.class));

            var passengers = InMemoryData.passengers();
            var flights = InMemoryData.flights();
            var bookings = InMemoryData.bookings();

            passengers.forEach(passengerTable::putItem);
            flights.forEach(flightTable::putItem);
            bookings.forEach(bookingTable::putItem);

            logSummary(passengers, flights, bookings);

            return true;
        } catch (Exception e) {
            System.out.println("❌ Error populating DynamoDB: " + e.getMessage());
            return false;
        }
    }

    private void logSummary(List<Passenger> passengers, List<Flight> flights, List<Booking> bookings) {
        System.out.println("\n=========== 🛫 Airline Data Summary 🛬 ===========\n");
        System.out.println(String.format("👤 Inserted Passengers: %d", passengers.size()));
        System.out.println(String.format("✈️ Inserted Flights: %d", flights.size()));
        System.out.println(String.format("📌 Inserted Bookings: %d", bookings.size()));

        System.out.println("\n===== ✈️ Flight Details =====");
        flights.forEach(this::logFlight);

        System.out.println("\n===== 📌 Booking Details =====");
        bookings.forEach(this::logBooking);
    }

    private void logFlight(Flight flight) {
        var primaryKey = flight.getPrimaryKey();
        System.out.println(String.format(
                "🛫 %s → 🛬 %s | Flight: %s | Model: %s | TotalSeats: %d | AvailableSeats: %d | ClaimedSeatMap: %s ",
                primaryKey.getSourceAirportCode(),
                primaryKey.getDestinationAirportCode(),
                flight.getFlightNumber(),
                flight.getAirplaneModel(),
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getClaimedSeatMap()
        ));
    }

    private void logBooking(Booking booking) {
        String departureDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(booking.getDepartureDateTime()), ZoneOffset.UTC).format(DATE_FORMATTER);
        System.out.println(String.format(
                "📌 Passenger: %-20s | Flight: %s | Seat: %s | Date: %s",
                booking.getCustomerEmail(),
                booking.getFlightNumber(),
                booking.getSeatNumber(),
                departureDate
        ));
    }
}
