package io.airlinesample.ddbops;

import io.airlinesample.ddbops.application.OptimisticLockingFlightBookingService;
import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import io.airlinesample.ddbops.persistence.SimpleClientBookFlightRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class simulates a **more efficient** flight booking process compared to the DynamoEnhancedClient, which uses **Optimistic Locking** with
 * the Simple DynamoDB Client API. While the DynamoDB client ensures data consistency, it introduces inefficiencies when compared to direct transaction management.
 * <p>
 * In this simulation:
 * <ul>
 *     <li>Two concurrent users attempt to book the same seat (2D) on the same flight, causing a conflict.</li>
 *     <li>Optimistic Locking is used to resolve conflicts, but it introduces overhead due to retries in case of conflicts.</li>
 *     <li>The **DynamoDB Client** is utilized, offering more flexibility, though the Version constraint must be managed manually.</li>
 * </ul>
 * <p>
 * This scenario highlights the **inefficiency** of the approach, where:
 * <ul>
 *     <li>Optimistic Locking introduces retries and revalidation, leading to increased processing time compared to simpler transaction-based approaches.</li>
 * </ul>
 */
public class SimpleClientOptimisticLockingBookFlightScenarioRunner {

    public static void main(String[] args) {

        try (var dynamoClient = AwsClientProvider.dynamoDbClient()) {

            var simpleClientBookFlightRepository = new SimpleClientBookFlightRepository(dynamoClient);
            var noLockingBookFlightUseCase = new OptimisticLockingFlightBookingService(simpleClientBookFlightRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();

            // Create booking tasks
            Runnable bookingTask = () -> {
                // Create a new booking that simulates a conflict (same seat)
                // London Heathrow (LHR) → Paris Charles de Gaulle (CDG), Departure DateTime: 2025-12-15T10:00
                // Passenger: Sherlock Homes
                // Seat: 4C
                Booking newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(UUID.randomUUID().toString())
                        .flightNumber("BA123")
                        .source("LHR")
                        .destination("CDG")
                        .departureDateTime(1765792800L)  // 2025-12-15T10:00
                        .seatNumber("4C")  // Conflict! Both users try to book seat
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = noLockingBookFlightUseCase.bookFlight(newBooking);
                System.out.println(Thread.currentThread().getName() + " - Booking success: " + success);
            };

            // Execute two concurrent booking tasks using CompletableFuture
            var futures = List.of(
                    CompletableFuture.runAsync(bookingTask),
                    CompletableFuture.runAsync(bookingTask)
            );

            // Wait for both tasks to complete before querying the updated flight
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        // Define the flight primary key for querying flight details after booking attempts
                        var primaryKey = FlightPrimaryKey.builder()
                                .sourceAirportCode("LHR")
                                .destinationAirportCode("CDG")
                                .departureDateTime(LocalDateTime.of(2025, 12, 15, 10, 0))
                                .build();

                        System.out.println("Database stats:");
                        // Use the enhanced client to query flight details after booking attempts
                        simpleClientBookFlightRepository.findFlight(primaryKey)
                                .ifPresent(System.out::println);

                        // Query and print the details of all attempted bookings
                        bookings.forEach(booking ->
                                simpleClientBookFlightRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresent(System.out::println));
                    })
                    .join(); // Ensure the main thread waits for all async tasks to finish
        }
    }
}
