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

### 3️⃣ Run App for initiating the Airline data
Run the following class to populate sample data:
```shell
io.airlinesample.ddbops.AirlineDynamoDbDataInitializer#main
```
Expected Output:
```shell
=========== 🛫 Airline Data Summary 🛬 ===========

👤 Inserted Passengers: 4
✈️ Inserted Flights: 5
📌 Inserted Bookings: 4

===== ✈️ Flight Details =====
🛫 LHR → 🛬 CDG | Flight: BA123 | Model: Airbus A320 | TotalSeats: 180 | AvailableSeats: 180 | ClaimedSeatMap: {} 
🛫 AMS → 🛬 FRA | Flight: KL456 | Model: Boeing 737-800 | TotalSeats: 189 | AvailableSeats: 150 | ClaimedSeatMap: {1A=0159b675-909c-72bc-bd49-07b67670039g} 
🛫 MAD → 🛬 LIS | Flight: IB789 | Model: Airbus A320 | TotalSeats: 180 | AvailableSeats: 60 | ClaimedSeatMap: {2B=0159b66b-9276-7e44-bd46-88565729fc71} 
🛫 FCO → 🛬 MUC | Flight: LH234 | Model: Airbus A320 | TotalSeats: 180 | AvailableSeats: 175 | ClaimedSeatMap: {3C=0159b66d-30dc-7de9-9671-46a139874465} 
🛫 BER → 🛬 VIE | Flight: OS567 | Model: Embraer E195 | TotalSeats: 120 | AvailableSeats: 2 | ClaimedSeatMap: {4D=0159b674-ddfs-7134-b45fe-8c1da462rec3} 

===== 📌 Booking Details =====
📌 Passenger: jxn.stove@email.com  | Flight: KL456 | Seat: 1A | Date: 2025-05-15 08:00:00
📌 Passenger: jxn.stove@email.com  | Flight: IB789 | Seat: 2B | Date: 2025-06-01 12:00:00
📌 Passenger: harry.soktor@email.com | Flight: LH234 | Seat: 3C | Date: 2025-08-01 14:15:00
📌 Passenger: harry.soktor@email.com | Flight: OS567 | Seat: 4D | Date: 2026-09-21 17:30:00
✅ Airline data inserted into DynamoDB successfully!
```

### 4️⃣ Run the Scenario Using Optimistic Locking (EnhancedClient, Less Efficient)
Run the class for the Optimistic Locking scenario using the Enhanced Client (less efficient implementation):
```shell
io.airlinesample.ddbops.EnhancedClientOptimisticLockingBookFlightScenarioRunner#main
```
Expected Output:
```shell
🚀 Starting Optimistic Locking Booking Scenario (using EnhancedClient) ...

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-2)

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-1)
✅ Flight booked successfully.
✅ Booking (ID: 3ffcc6c1-0680-44e1-ac30-ef79c9decd83) result: true [Thread: ForkJoinPool.commonPool-worker-2]
❌ Booking (ID: 3bddcef5-1276-4e13-af02-d99aa4568e07) result: false [Thread: ForkJoinPool.commonPool-worker-1]

📊 Fetching updated flight and booking details...

✈️ Updated Flight Information:
📌 Flight(routeByDay=AMS#FRA#2025-05-15, departureTime=0800, flightNumber=KL456, airplaneModel=Boeing 737-800, totalSeats=189, availableSeats=149, heldSeats=10, version=2, claimedSeatMap={1A=0159b675-909c-72bc-bd49-07b67670039g, 2D=3ffcc6c1-0680-44e1-ac30-ef79c9decd83})

📌 Attempted Bookings:
⚠️ Optimistic locking failed: Another user modified the flight concurrently. Consider attempting again...
✅ Booking(customerEmail=sherlock.homes@email.com, bookingID=3ffcc6c1-0680-44e1-ac30-ef79c9decd83, flightNumber=KL456, source=AMS, destination=FRA, departureDateTime=1747296000, seatNumber=2D, fareClass=Economy)
❌ Booking not found in DB: 3bddcef5-1276-4e13-af02-d99aa4568e07

🏁 Booking scenario completed.
```

### 5️⃣ Run the Scenario Using Optimistic Locking (DynamoClient, More Efficient)
Run the class for the Optimistic Locking scenario using the DynamoDB Client (a bit more efficient due to expression projections):
```shell
io.airlinesample.ddbops.SimpleClientOptimisticLockingBookFlightScenarioRunner#main
```
Expected Output:
```shell
🚀 Starting Simple Client Optimistic Locking Booking Scenario (using DynamoClient) ...

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-1)

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-2)
✅ Flight booked successfully.
✅ Booking (ID: 2a85b26b-61b9-45de-a00e-5169dc6e2e8a) result: true [Thread: ForkJoinPool.commonPool-worker-2]
⚠️ Optimistic locking failed: Another user modified the flight concurrently. Consider attempting again...
❌ Booking (ID: 4a0278d6-c333-480b-8ec1-e0bd0467c947) result: false [Thread: ForkJoinPool.commonPool-worker-1]

📊 Fetching updated flight and booking details...

✈️ Updated Flight Information:
📌 Flight(routeByDay=null, departureTime=null, flightNumber=null, airplaneModel=null, totalSeats=180, availableSeats=179, heldSeats=0, version=2, claimedSeatMap={4C=2a85b26b-61b9-45de-a00e-5169dc6e2e8a})

📌 Attempted Bookings:
❌ Booking not found in DB: 4a0278d6-c333-480b-8ec1-e0bd0467c947
✅ Booking(customerEmail=sherlock.homes@email.com, bookingID=2a85b26b-61b9-45de-a00e-5169dc6e2e8a, flightNumber=null, source=null, destination=null, departureDateTime=1765792800, seatNumber=null, fareClass=null)

🏁 Booking scenario completed.

```

### 6️⃣ Run the Scenario Using Conditional Expressions (Most Efficient Implementation)
Run the class for the Conditional Expression scenario using the DynamoDB Client (most efficient implementation, avoids reads and enforces data integrity via condition expressions):
```shell
io.airlinesample.ddbops.BestPracticeBookFlightScenarioRunner#main
```
Expected Output:
```shell
🚀 Starting Best Practice Flight Booking Scenario (using DynamoClient with ConditionalExpression) ...

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-1)

🛫 Attempting to book a flight (Thread: ForkJoinPool.commonPool-worker-2)
✅ Flight booked successfully.
✅ Booking (ID: bb9310d9-34c6-477e-82ef-eb9d1d55979e) result: true [Thread: ForkJoinPool.commonPool-worker-1]
⚠️ No seats available or specified seat already taken.
❌ Booking (ID: 16c14775-454f-4039-90db-bda89f7fe845) result: false [Thread: ForkJoinPool.commonPool-worker-2]

📊 Fetching updated flight and booking details...

✈️ Updated Flight Information:
❌ Optimistic locking failed: Another user modified the flight concurrently. Consider attempting again...
📌 Flight(routeByDay=LHR#CDG#2025-12-15, departureTime=1000, flightNumber=BA123, airplaneModel=Airbus A320, totalSeats=180, availableSeats=178, heldSeats=0, version=3, claimedSeatMap={2C=bb9310d9-34c6-477e-82ef-eb9d1d55979e, 4C=2a85b26b-61b9-45de-a00e-5169dc6e2e8a})

📌 Attempted Bookings:
✅ Booking(customerEmail=sherlock.homes@email.com, bookingID=bb9310d9-34c6-477e-82ef-eb9d1d55979e, flightNumber=BA123, source=LHR, destination=CDG, departureDateTime=1765792800, seatNumber=2C, fareClass=Economy)
❌ Booking not found in DB: 16c14775-454f-4039-90db-bda89f7fe845

🏁 Booking scenario completed.
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
