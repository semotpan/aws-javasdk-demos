package io.airlinesample.ddbops.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Passenger {

    public static final String PASSENGER_TABLE_NAME = "passengers";

    public static final String EMAIL_ADDRESS_FIELD_NAME = "EmailAddress";
    public static final String FULL_NAME_FIELD_NAME = "FullName";
    public static final String BIRTHDAY_FIELD_NAME = "Birthday";
    public static final String FREQUENT_FLYER_ID_FIELD_NAME = "FrequentFlyerID";
    public static final String PREFERENCES_FIELD_NAME = "Preferences";

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(EMAIL_ADDRESS_FIELD_NAME)
    }))
    @JsonProperty(EMAIL_ADDRESS_FIELD_NAME)
    private String emailAddress;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(FULL_NAME_FIELD_NAME)
    }))
    @JsonProperty(FULL_NAME_FIELD_NAME)
    private String fullName;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(BIRTHDAY_FIELD_NAME)
    }))
    @JsonProperty(BIRTHDAY_FIELD_NAME)
    private Long birthday;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(FREQUENT_FLYER_ID_FIELD_NAME)
    }))
    @JsonProperty(FREQUENT_FLYER_ID_FIELD_NAME)
    private String frequentFlyerID;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(PREFERENCES_FIELD_NAME)
    }))
    @JsonProperty(PREFERENCES_FIELD_NAME)
    private Preferences preferences;

    @Builder
    public Passenger(String emailAddress, String fullName, Long birthday, String frequentFlyerID, Preferences preferences) {
        this.emailAddress = emailAddress;
        this.fullName = fullName;
        this.birthday = birthday;
        this.frequentFlyerID = frequentFlyerID;
        this.preferences = preferences;
    }
}
