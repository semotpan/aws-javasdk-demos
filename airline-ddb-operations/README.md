# 🚀 DynamoDB Operations with LocalStack for Airline Use-Cases

### Analyzed Use Case: Booking a Flight (with or without a specified seat)

###### _Tech Stack: Java 21, AWS SDK, and LocalStack_

---

This use case demonstrates how to implement AWS DynamoDB transactions using the [Java SDK V2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/welcome.html), `DynamoClient`, and `EnhancedDynamoClient`. It covers three scenarios for implementing transactional integrity:

1. **EnhancedDynamoClient with `@DynamoDBAttributeVersion`**: This scenario delegates data integrity to `EnhancedDynamoClient` with its built-in mechanism, which adds version checking on expression conditions. Learn more in the [Enhanced Client Documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.HowItWorks.html).

2. **DynamoClient with manual versioning**: This approach mimics the behavior of `EnhancedDynamoClient` using the `@DynamoDBAttributeVersion`, but it gives more control over features like `ProjectionExpression`, improving the cost efficiency of consistent reads. For more on DynamoDB Expressions, check the [DynamoDB Expressions Guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.html).

3. **Best practices for DynamoDB**: This scenario avoids the anti-pattern of the `read-modify-write` cycle. It does not perform any checks on the current version but relies entirely on DynamoDB for data integrity via expression conditions. This is the most cost-efficient implementation, but it requires careful data model design to ensure its effectiveness. For best practices in DynamoDB, refer to [Best Practices for DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/BestPractices.html).

### Context:

> **Use case**: _As a passenger, I want to book a flight with a specified seat or not._

### Task:
Use DynamoDB transactions to ensure data consistency. For more on DynamoDB transactions, visit the [Transactions Guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Transactions.html) and the [DynamoDB TransactWriteItems API Reference](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactWriteItems.html).

#### Database Schema
![database-schema.png](database-schema.png)

#### DynamoDB Workflow Operations
![booking-workflows.png](booking-workflows.png)

## 📌 Tech Stack

- **Java 21** (via [SDKMAN!](https://sdkman.io/))
- **Maven Wrapper** for build automation
- **Docker Compose** for container orchestration
- **AWS CLI** (configured for LocalStack for local AWS simulation)

## 🏗️ Setup & Usage

### 0️⃣ Configure AWS CLI for LocalStack

Before running the project, ensure that the AWS CLI is configured to interact with LocalStack. Run the following commands:

```bash
aws configure set aws_access_key_id "key" --profile localstack
aws configure set aws_secret_access_key "secret" --profile localstack
aws configure set region "eu-west-1" --profile localstack
aws configure set output "json" --profile localstack
```

### 1️⃣ Start LocalStack with Docker Compose

Ensure Docker is running, then execute:
```shell
docker compose -f compose.yaml up -d
```

### 2️⃣ Create CloudFormation Stack

Deploy the CloudFormation stack to provision DynamoDB tables:
```shell
aws --profile localstack --endpoint-url=http://localhost:4566 cloudformation deploy \
    --template-file  cfTemplate.yaml \
    --stack-name airline-ddb-ops
```

### 3️⃣ Run App Data Population
Run the following class to populate sample data:
```shell
io.airlinesample.ddbops.SampleDataDynamoPopulate#main
```
Expected Output:
```shell
In memory data inserted!
```

### 4️⃣ Run the Scenario Using Optimistic Locking (EnhancedClient, Less Efficient)
Run the class for the Optimistic Locking scenario using the Enhanced Client (less efficient implementation):
```shell
io.airlinesample.ddbops.EnhancedClientOptimisticLockingBookFlightScenarioRunner#main
```
Expected Output:
```shell
Flight booked successfully.
ForkJoinPool.commonPool-worker-2 - Booking success: true
Optimistic locking failed: Another user modified the flight concurrently. Consider attemting again ...
ForkJoinPool.commonPool-worker-1 - Booking success: false
Database stats:
Flight(routeByDay=AMS#FRA#2025-05-15, departureTime=0800, flightNumber=KL456, airplaneModel=Boeing 737-800, totalSeats=189, availableSeats=149, heldSeats=10, version=2, claimedSeatMap={1A=0159b675-909c-72bc-bd49-07b67670039g, 2D=b155feaa-b938-4992-bdd8-35395dccc47d})
Booking(customerEmail=sherlock.homes@email.com, bookingID=b155feaa-b938-4992-bdd8-35395dccc47d, flightNumber=KL456, source=AMS, destination=FRA, departureDateTime=1747296000, seatNumber=2D, fareClass=Economy)
```

### 5️⃣ Run the Scenario Using Optimistic Locking (DynamoClient, More Efficient)
Run the class for the Optimistic Locking scenario using the DynamoDB Client (a bit more efficient due to expression projections):
```shell
io.airlinesample.ddbops.SimpleClientOptimisticLockingBookFlightScenarioRunner#main
```
Expected Output:
```shell
Flight booked successfully.
ForkJoinPool.commonPool-worker-1 - Booking success: true
Optimistic locking failed: Another user modified the flight concurrently. Consider attemting again ...
ForkJoinPool.commonPool-worker-2 - Booking success: false
Database stats:
Flight(routeByDay=null, departureTime=null, flightNumber=null, airplaneModel=null, totalSeats=180, availableSeats=179, heldSeats=0, version=2, claimedSeatMap={4C=e594fd16-6462-4acd-906e-75ef290f45f5})
Booking(customerEmail=sherlock.homes@email.com, bookingID=e594fd16-6462-4acd-906e-75ef290f45f5, flightNumber=null, source=null, destination=null, departureDateTime=1765792800, seatNumber=null, fareClass=null)
```

### 6️⃣ Run the Scenario Using Conditional Expressions (Most Efficient Implementation)
Run the class for the Conditional Expression scenario using the DynamoDB Client (most efficient implementation, avoids reads and enforces data integrity via condition expressions):
```shell
io.airlinesample.ddbops.BestPracticeBookFlightScenarioRunner#main
```
Expected Output:
```shell
Flight booked successfully.
ForkJoinPool.commonPool-worker-2 - Booking success: true
No seats available or specified seat already taken.
ForkJoinPool.commonPool-worker-1 - Booking success: false
Database stats:
Optimistic locking failed: Another user modified the flight concurrently. Consider attemting again ...
Flight(routeByDay=LHR#CDG#2025-12-15, departureTime=1000, flightNumber=BA123, airplaneModel=Airbus A320, totalSeats=180, availableSeats=178, heldSeats=0, version=3, claimedSeatMap={2C=20b7981d-a4f5-4adc-93f2-2f5032a8fce8, 4C=e594fd16-6462-4acd-906e-75ef290f45f5})
Booking(customerEmail=sherlock.homes@email.com, bookingID=20b7981d-a4f5-4adc-93f2-2f5032a8fce8, flightNumber=BA123, source=LHR, destination=CDG, departureDateTime=1765792800, seatNumber=2C, fareClass=Economy)`
```

### 7️⃣ Delete CloudFormation Stack

Run the following AWS CLI command to delete the CloudFormation stack:
```shell
aws --profile localstack --endpoint-url=http://localhost:4566 cloudformation \
delete-stack --stack-name airline-ddb-ops
```

### 8️⃣ Tear Down Docker Compose with Volumes
If Docker is running, execute the following to stop and remove the containers and volumes:
```shell
docker compose -f compose.yaml down -v
```
