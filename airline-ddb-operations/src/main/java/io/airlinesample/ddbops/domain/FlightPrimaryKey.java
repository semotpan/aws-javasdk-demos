package io.airlinesample.ddbops.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
public class FlightPrimaryKey {

    public static final String DEPARTURE_DATE_FORMATTER = "yyyy-MM-dd";
    public static final String DEPARTURE_TIME_FORMATTER = "HHmm";

    // Regex pattern to validate the partition key (sourceAirportCode#destinationAirportCode#date, e.g. KIV#LIS#2030-06-12)
    public static final String PARTITION_KEY_PATTERN = "^[A-Z]{3}#[A-Z]{3}#\\d{4}-\\d{2}-\\d{2}$";
    private static final Pattern pkPattern = Pattern.compile(PARTITION_KEY_PATTERN);

    // Regex pattern to validate the sort key (HHmm)
    public static final String SORT_KEY_PATTERN = "^(0[0-9]|1[0-9]|2[0-3])[0-5][0-9]$";
    private static final Pattern skPattern = Pattern.compile(SORT_KEY_PATTERN);

    String partitionKey;
    String sortKey;

    String sourceAirportCode;
    String destinationAirportCode;
    LocalDateTime departureDateTime;

    @Builder
    public FlightPrimaryKey(String sourceAirportCode, String destinationAirportCode, LocalDateTime departureDateTime) {
        requireNonNull(sourceAirportCode, "sourceAirportCode cannot be null");
        requireNonNull(destinationAirportCode, "destinationAirportCode cannot be null");
        requireNonNull(departureDateTime, "departureDateTime cannot be null");

        var departureDate = departureDateTime.toLocalDate().format(DateTimeFormatter.ofPattern(DEPARTURE_DATE_FORMATTER));
        // PK: sourceAirportCode#destinationAirportCode#date, e.g. KIV#LIS#2030-06-12
        this.partitionKey = String.join("#", sourceAirportCode, destinationAirportCode, departureDate);

        // SK: HHmm, e.g. 0800
        this.sortKey = departureDateTime.toLocalTime().format(DateTimeFormatter.ofPattern(DEPARTURE_TIME_FORMATTER));

        this.sourceAirportCode = sourceAirportCode;
        this.destinationAirportCode = destinationAirportCode;
        this.departureDateTime = departureDateTime;
    }

    @Builder
    public FlightPrimaryKey(String partitionKey, String sortKey) {
        requireNonNull(partitionKey, "partitionKey cannot be null");
        requireNonNull(sortKey, "sortKey cannot be null");

        if (!pkPattern.matcher(partitionKey).matches()) {
            throw new IllegalArgumentException("Invalid partition key: " + partitionKey + ", valid pattern e.g. 'sourceAirportCode#destinationAirportCode#date'.");
        }

        this.partitionKey = partitionKey;

        if (!skPattern.matcher(sortKey).matches()) {
            throw new IllegalArgumentException("Invalid sort key: " + sortKey + ", it should be valid minutes and seconds (e.g. 0840).");
        }
        this.sortKey = sortKey;


        var pkSlit = partitionKey.split("#");


        this.sourceAirportCode = pkSlit[0];
        this.destinationAirportCode = pkSlit[1];
        this.departureDateTime = LocalDateTime.of(LocalDate.parse(pkSlit[2], DateTimeFormatter.ofPattern(DEPARTURE_DATE_FORMATTER)), LocalTime.parse(sortKey, DateTimeFormatter.ofPattern(DEPARTURE_TIME_FORMATTER)));
    }
}
