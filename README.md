# Seat Reservation at Scale

A concurrent seat reservation backend built with Java and Spring Boot.

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- H2
- Maven
- Micrometer
- Prometheus
[app](src/main/java/com/booking/app)
## Core Guarantees

- No double-selling of seats
- Per-user booking limit
- Idempotent reservations
- All-or-nothing multi-seat booking
- Safe cancellation
- Health checks
- Prometheus metrics
- Structured request IDs
