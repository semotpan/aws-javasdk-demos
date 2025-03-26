# AWS Java SDK Use Case

This repository showcases different use cases for the AWS Java SDK, focusing on DynamoDB operations.

---

## Projects

- **airline-ddb-operations**: A project for flight booking with two DynamoDB implementations:
  - **DynamoClient**: A low-level approach providing fine-grained control over operations.
  - **EnhancedDynamoClient**: A higher-level approach simplifying operations and automatic version handling.

  Both implementations are compared for their pros and cons, highlighting use cases such as:
  - Booking flights with or without specified seats.
  - Ensuring transactional consistency across flight, passenger, and booking data.
  - Using best practices to avoid the `read-modify-write` cycle and optimize costs.

---

## Resources

- [AWS Java SDK Documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/welcome.html)
- [Amazon DynamoDB Documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Welcome.html)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/BestPractices.html)
