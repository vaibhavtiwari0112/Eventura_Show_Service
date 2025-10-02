# Eventura - Show Service

This microservice manages showtimes and publishes show-created events to Kafka. It delegates seat layout queries to the Catalog service and (optionally) queries Booking service for current locks.

## Features
- Create shows (admin)
- List and fetch shows
- Get seatmap (merged from Catalog + Booking locks)
- Publishes `show.events` topic when a show is created

## Run locally
Requirements: Java 17, Maven, Postgres, Kafka

1. Start Postgres and Kafka (you can use docker-compose from your repo).
2. Build and run:
   ```
   mvn clean package
   mvn spring-boot:run
   ```
3. Environment variables:
   - `JDBC_DATABASE_URL` (default jdbc:postgresql://localhost:5432/eventura)
   - `JDBC_DATABASE_USERNAME` (default postgres)
   - `JDBC_DATABASE_PASSWORD` (default password)
   - `KAFKA_BOOTSTRAP` (default localhost:9092)
   - `CATALOG_SERVICE_URL` (default http://localhost:8083)
   - `BOOKING_SERVICE_URL` (default http://localhost:8082)

## API
- `POST /api/shows` - create show. Body:
  ```json
  {
    "movieId": "uuid",
    "hallId": "uuid",
    "startTime": "2025-09-15T18:30:00+05:30",
    "basePrice": 199.50
  }
  ```
- `GET /api/shows` - list shows
- `GET /api/shows/{id}` - show details
- `GET /api/shows/{id}/seatmap` - returns catalog hall layout and booking locks (if Booking service available)

## Notes on integration
- Catalog service stores hall seat layouts and returns JSON for `/api/catalog/halls/{hallId}`.
- Booking service exposes an endpoint to query locks; this service attempts to call `/api/bookings/locks?showId=...` and will ignore failures (so show creation remains available).
- Events are published to Kafka topic `show.events`.

