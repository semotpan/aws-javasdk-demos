package io.airlinesample.ddbops;

import io.airlinesample.ddbops.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class InMemoryData {

    static List<Passenger> passengers() {
        return List.of(
                Passenger.builder()
                        .emailAddress("jxn.stove@email.com")
                        .fullName("Jon Snow")
                        .birthday(157766400L) // Wednesday, 1 January 1975 00:00:00
                        .frequentFlyerID("AMS4564")
                        .preferences(Preferences.builder()
                                .seatPreference("Window")
                                .timezone("Europe/Bucharest")
                                .build())
                        .build(),
                Passenger.builder()
                        .emailAddress("harry.soktor@email.com")
                        .fullName("Harry Potter")
                        .birthday(1183766400L) // Saturday, 7 July 2007 00:00:00
                        .frequentFlyerID("BASS4565")
                        .preferences(Preferences.builder()
                                .seatPreference("Window")
                                .mealPreference(List.of("Vegan"))
                                .timezone("Europe/Chisinau")
                                .build())
                        .build(),
                Passenger.builder()
                        .emailAddress("vlad.topee@gmail.com")
                        .fullName("Vlad Tapes")
                        .birthday(-11676096000L) // Saturday, 1 January 1600 00:00:00
                        .frequentFlyerID("C44455778")
                        .preferences(Preferences.builder()
                                .seatPreference("Aisle")
                                .language("Spanish")
                                .timezone("Europe/Bucharest")
                                .build())
                        .build(),
                Passenger.builder()
                        .emailAddress("sherlock.homes@email.com")
                        .fullName("Sherlock Homes")
                        .birthday(-157766400L) // Friday, 1 January 1965 00:00:00
                        .frequentFlyerID("D73979865")
                        .preferences(Preferences.builder()
                                .mealPreference(List.of("Gluten-Free"))
                                .language("English")
                                .accessibilityRequirements(List.of("Wheelchair Access"))
                                .build())
                        .build()
        );
    }

    static List<Flight> flights() {
        return List.of(
                // London Heathrow (LHR) → Paris Charles de Gaulle (CDG)
                // Source Airport Code: LHR
                // Destination Airport Code: CDG
                // Departure DateTime: 2025-12-15T10:00 = 1765792800L Epoch timestamp seconds
                // Flight Number: BA123
                // Airplane Model: Airbus A320
                // Total Seats: 180
                Flight.builder()
                        .primaryKey(FlightPrimaryKey.builder()
                                .sourceAirportCode("LHR")
                                .destinationAirportCode("CDG")
                                .departureDateTime(LocalDateTime.of(2025, 12, 15, 10, 0))
                                .build())
                        .airplaneModel("Airbus A320")
                        .flightNumber("BA123")
                        .totalSeats(180)
                        .build(),

                // Amsterdam Schiphol (AMS) → Frankfurt Airport (FRA)
                // Source Airport Code: AMS
                // Destination Airport Code: FRA
                // Departure DateTime: 2025-05-15T08:00 = 1747296000L Epoch timestamp seconds
                // Flight Number: KL456
                // Airplane Model: Boeing 737-800
                // Total Seats: 189
                Flight.mapBuilder()
                        .routeByDay("AMS#FRA#2025-05-15")
                        .departureTime("0800")
                        .flightNumber("KL456")
                        .airplaneModel("Boeing 737-800")
                        .totalSeats(189)
                        .availableSeats(150)
                        .heldSeats(10)
                        .claimedSeatMap(Map.of("1A", "0159b675-909c-72bc-bd49-07b67670039g"))
                        .build(),

                // Madrid Barajas (MAD) → Lisbon Humberto Delgado Airport (LIS)
                // Source Airport Code: MAD
                // Destination Airport Code: LIS
                // Departure DateTime: 2025-06-01T12:00 = 1748779200L Epoch timestamp seconds
                // Flight Number: IB789
                // Airplane Model: Airbus A320
                // Total Seats: 180
                Flight.mapBuilder()
                        .routeByDay("MAD#LIS#2025-06-01")
                        .departureTime("1200")
                        .flightNumber("IB789")
                        .airplaneModel("Airbus A320")
                        .totalSeats(180)
                        .availableSeats(60)
                        .heldSeats(5)
                        .claimedSeatMap(Map.of("2B", "0159b66b-9276-7e44-bd46-88565729fc71"))
                        .build(),
                // Rome Fiumicino (FCO) → Munich Airport (MUC)
                // Source Airport Code: FCO
                // Destination Airport Code: MUC
                // Departure DateTime: 2025-08-01T14:15 = 1754057700L Epoch timestamp seconds
                // Flight Number: LH234
                // Airplane Model: Airbus A320
                // Total Seats: 180
                Flight.mapBuilder()
                        .routeByDay("FCO#MUC#2025-08-01")
                        .departureTime("1415")
                        .flightNumber("LH234")
                        .airplaneModel("Airbus A320")
                        .totalSeats(180)
                        .availableSeats(175)
                        .heldSeats(3)
                        .claimedSeatMap(Map.of("3C", "0159b66d-30dc-7de9-9671-46a139874465"))
                        .build(),
                // Berlin Brandenburg (BER) → Vienna International Airport (VIE)
                // Source Airport Code: BER
                // Destination Airport Code: VIE
                // Departure DateTime: 2026-09-21T17:30  = 1790011800L Epoch timestamp seconds
                // Flight Number: OS567
                // Airplane Model: Embraer E195
                // Total Seats: 120
                Flight.mapBuilder()
                        .routeByDay("BER#VIE#2026-09-21")
                        .departureTime("1730")
                        .flightNumber("OS567")
                        .airplaneModel("Embraer E195")
                        .totalSeats(120)
                        .availableSeats(2)
                        .heldSeats(118)
                        .claimedSeatMap(Map.of("4D", "0159b674-ddfs-7134-b45fe-8c1da462rec3"))
                        .build()
        );
    }

    static List<Booking> bookings() {
        return List.of(
                // Amsterdam Schiphol (AMS) → Frankfurt Airport (FRA)
                Booking.builder()
                        .customerEmail("jxn.stove@email.com")
                        .bookingID("0159b675-909c-72bc-bd49-07b67670039g")
                        .flightNumber("KL456")
                        .source("AMS")
                        .destination("FRA")
                        .departureDateTime(1747296000L)
                        .seatNumber("1A")
                        .fareClass("Economy")
                        .build(),
                // Madrid Barajas (MAD) → Lisbon Humberto Delgado Airport (LIS)
                Booking.builder()
                        .customerEmail("jxn.stove@email.com")
                        .bookingID("0159b66b-9276-7e44-bd46-88565729fc71")
                        .flightNumber("IB789")
                        .source("MAD")
                        .destination("LIS")
                        .departureDateTime(1748779200L)
                        .seatNumber("2B")
                        .fareClass("Business")
                        .build(),
                // Rome Fiumicino (FCO) → Munich Airport (MUC)
                Booking.builder()
                        .customerEmail("harry.soktor@email.com")
                        .bookingID("0159b66d-30dc-7de9-9671-46a139874465")
                        .flightNumber("LH234")
                        .source("FCO")
                        .destination("MUC")
                        .departureDateTime(1754057700L)
                        .seatNumber("3C")
                        .fareClass("Economy Plus")
                        .build(),
                // Berlin Brandenburg (BER) → Vienna International Airport (VIE)
                Booking.builder()
                        .customerEmail("harry.soktor@email.com")
                        .bookingID("0159b674-ddfs-7134-b45fe-8c1da462rec3")
                        .flightNumber("OS567")
                        .source("BER")
                        .destination("VIE")
                        .departureDateTime(1790011800L)
                        .seatNumber("4D")
                        .fareClass("Economy")
                        .build()
        );
    }
}
