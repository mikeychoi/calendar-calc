CLAUDE.md — Date Calculator API
1. Project Overview
Build a RESTful Date Calculator API using Java and Spring Boot.
The application calculates dates using either calendar days or business days. Business-day calculations must exclude weekends and holidays stored in a Holiday Calendar.
This project is a technical assessment. The priorities are:
Correct and reliable date calculations.
Clean, understandable, maintainable code.
Proper REST API design and HTTP status codes.
Input validation and consistent error handling.
Unit tests for core functionality.
Clear API documentation and an easy-to-follow demo.
Keep the implementation appropriately scoped for a small assessment. Avoid unnecessary complexity, additional frameworks, microservices, or infrastructure.
2. Assessment Requirements
Required functionality
Date calculations
Add a specified number of calendar days to a starting date.
Add a specified number of business days to a starting date.
Business days exclude Saturdays, Sundays, and configured holidays.
Validate input, including date formats and required fields.
Subtract calendar or business days from a starting date.
Calculate the number of calendar days and business days between two dates.
Holiday Calendar
Manage the 2027 Holiday Calendar.
Add holidays.
Retrieve holidays.
Update holidays.
Remove holidays.
Other
Handle invalid input and unexpected exceptions appropriately.
Write unit tests for date calculations and holiday-calendar functionality.
3. Technology Stack
Use the following technologies unless there is a compelling reason to change them:
Java — primary programming language.
Spring Boot — application framework and REST API.
Spring Web — REST controllers and HTTP handling.
Spring Data JPA — database access and repository abstraction.
H2 Database — lightweight, file-based database.
Maven — dependency management and build tool.
JUnit 5 — unit testing.
Mockito — mocking dependencies in unit tests where appropriate.
springdoc-openapi (3.x, for Spring Boot 4) — generates OpenAPI docs from the controllers at runtime. Swagger UI at /swagger-ui.html, raw spec at /v3/api-docs. Code annotations are the source of truth; no hand-maintained spec file.
Use Java's built-in java.time API, particularly LocalDate and DayOfWeek, for date operations.
Avoid legacy date APIs such as java.util.Date and Calendar unless absolutely necessary.
Project setup (confirmed)
Java 21 (LTS), Spring Boot 4.1.2, generated with Spring Initializr.
Build with the Maven wrapper (./mvnw); a global Maven install is not required.
Base package: com.mikeychoi.calendarcalc. Main class: CalendarCalcApplication.
Spring Boot 4 uses modular starters (e.g. spring-boot-starter-webmvc, spring-boot-h2console, *-test starters).
Confirmed decisions
The holiday calendar is not restricted to 2027. Holidays may be added for any year; ?year= is only a filter.
GET /v1/holidays without a year parameter returns all holidays. year must be an integer from 1 to 9999 (@Min/@Max); otherwise 400.
Exact duplicate holidays (same date AND same name) are rejected with 409 Conflict. Different holidays on the same date are allowed.
Name comparison for duplicates ignores capitalization and leading/trailing spaces ("Christmas" == "  CHRISTMAS "). Names are stored trimmed with original capitalization.
Duplicates are enforced twice: a service-level check (clean 409 message) and a database unique constraint on (holiday_date, normalized_name) as a backstop. normalized_name is internal and never exposed in the API.
numDays must be between 0 and 100000 inclusive (@Min(0), @Max(100000)). Results outside LocalDate's supported range return 400, not 500.
PATCH: a missing or null field means "do not change". A blank name ("") is rejected with 400.
Tests use an in-memory H2 database (src/test/resources) so they never touch ./data/.
The developer handles all git commits. Claude should not commit.
4. Architecture and Code Organization
Use a straightforward layered architecture:
Client → Controller → Service → Repository → Database
Each layer should have a clearly defined responsibility.
Controller
Defines HTTP endpoints.
Accepts and returns HTTP requests/responses.
Performs request-level validation through DTOs and annotations.
Delegates business logic to services.
Should not contain date-calculation algorithms or database logic.
Service
Contains the application's business logic.
Performs calendar-day and business-day calculations.
Coordinates holiday-calendar operations.
Handles business rules and raises appropriate exceptions.
Should not directly construct HTTP responses.
Repository
Handles database access through Spring Data JPA.
Provides methods for retrieving, saving, updating, and deleting holiday records.
Entity
Represents a database record.
Use JPA annotations to define persistence behavior.
DTO (Data Transfer Object)
Defines the structure of API requests and responses.
Keeps API contracts separate from database entities.
Avoid returning JPA entities directly from controllers.
Exception Handling
Use a centralized @RestControllerAdvice.
Convert application exceptions into consistent HTTP error responses.
Keep HTTP-specific error handling out of service classes.
A possible package structure:
calendar-calc
│
├── controller/
│   ├── DateCalculationController.java
│   └── HolidayController.java
│
├── service/
│   ├── DateCalculationService.java
│   └── HolidayService.java
│
├── repository/
│   └── HolidayRepository.java
│
├── entity/
│   └── Holiday.java
│
├── dto/
│   ├── AddDateRequest.java
│   ├── DateCalculationResponse.java
│   ├── DateDifferenceRequest.java
│   ├── DateDifferenceResponse.java
│   ├── HolidayRequest.java
│   ├── HolidayResponse.java
│   └── ApiError.java
│
├── exception/
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
│
└── CalendarCalcApplication.java
This is a suggested structure, not a requirement to create every file immediately. Introduce classes as they become necessary.
5. Database Design
Use a file-based H2 database so holiday records persist when the application restarts.
Suggested JDBC URL:
spring.datasource.url=jdbc:h2:file:./data/calendar-calc
Use Spring Data JPA for persistence.
Holiday entity
A holiday should contain:
id: Unique identifier, generated by the database.
date: The holiday's date, represented by LocalDate.
name: A human-readable holiday name.
Example:
@Entity
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private String name;
}
Use appropriate constructors, getters, and setters or another simple approach consistent with the project.
Holiday identity and duplicates
Use the unique ID as the identity of a holiday, rather than using its date as its identifier.
Multiple holiday records may have the same date. This allows two distinct holiday entries to exist on the same day.
For business-day calculations, convert holiday dates into a Set<LocalDate>. A date should only be excluded once, even if multiple holiday records share it.
Persistence configuration
For local development, use Hibernate schema generation with:
spring.jpa.hibernate.ddl-auto=update
Keep the local database directory out of version control by adding data/ to .gitignore.
Do not introduce a separate database server or Docker setup unless there is a specific need.
6. API Design
Use the /v1 prefix for all endpoints. Section 10 is the authoritative endpoint list.
All dates should use ISO 8601 format:
YYYY-MM-DD
Example:
2027-12-25
Use JSON for request and response bodies.
A. Date calculations
Add days
POST /v1/date/add
Request:
{
  "startDate": "2026-11-07",
  "numDays": 10,
  "calcType": "CALENDAR"
}
Response:
{
  "startDate": "2026-11-07",
  "resultDate": "2026-11-17",
  "numDays": 10,
  "calcType": "CALENDAR"
}
Supported calculation types:
public enum CalculationType {
    CALENDAR,
    BUSINESS
}
Subtract days
POST /v1/date/subtract
Use the same request and response DTO structure as the add endpoint.
Calculate date difference 
POST /v1/date/difference
Request:
{
  "startDate": "2026-11-07",
  "endDate": "2026-11-17"
}
Response:
{
  "startDate": "2026-11-07",
  "endDate": "2026-11-17",
  "calendarDays": 10,
  "businessDays": 7
}
The example assumes there are no holidays in the interval.
Return both calendar-day and business-day counts. Do not require a calculation type for this endpoint.
B. Holiday management
Retrieve holidays
GET /v1/holidays?year=2027
Returns all holiday records for the requested year.
If no holidays exist, return an empty array.
Retrieve one holiday (optional)
GET /v1/holidays/{id}
Add a holiday
POST /v1/holidays
Request:
{
  "date": "2027-12-25",
  "name": "Christmas"
}
The server should generate the ID.
Return 201 Created, the created holiday, and ideally a Location header pointing to the new resource.
Update a holiday
PATCH /v1/holidays/{id}
Use partial-update semantics. Only supplied fields should change.
Return the updated holiday.
Delete a holiday
DELETE /v1/holidays/{id}
Return 204 No Content on success.
API design conventions
Use nouns for resource URLs, such as /holidays.
Use HTTP methods according to their intended behavior.
Use plural resource names.
Keep request and response structures consistent.
Do not expose database implementation details through the API.
7. Date Calculation Rules
These rules must be applied consistently throughout the application.
Calendar days
Every calendar day counts, including Saturdays, Sundays, and holidays.
Use:
date.plusDays(numberOfDays);
Business days
A business day is any day that:
Is not Saturday.
Is not Sunday.
Is not present in the Holiday Calendar.
Use:
date.getDayOfWeek()
to determine the weekday.
Business-day calculation convention
The starting date is excluded from the count.
For example, adding one business day to a Friday returns the following Monday, assuming Monday is not a holiday.
Adding zero days returns the original date.
A straightforward algorithm is preferred:
Start with the input date.
Move forward one calendar day.
Check whether the new date is a business day.
If it is, increment the business-day counter.
Repeat until the requested number of business days has been reached.
For subtraction, use the same general approach in reverse.
Date difference convention
For the optional difference endpoint, use the interval:
(startDate, endDate]
This means:
Exclude the starting date.
Include the ending date.
For example, the calendar-day difference from November 7 to November 17 is 10.
Business-day calculations must use the same interval convention.
Document these conventions in the API documentation.
8. Validation and Error Handling
Validate input at the API boundary using Jakarta Bean Validation and @Valid.
Examples:
startDate and other required dates must not be null.
numDays must be greater than or equal to zero.
calcType must be a valid enum value.
year must be a valid integer.
A date-difference request must have startDate less than or equal to endDate.
Holiday IDs must refer to existing records for update/delete operations.
Use appropriate HTTP status codes:
Status
Meaning
200
Successful retrieval, calculation, or update
201
Resource successfully created
204
Resource successfully deleted
400
Invalid request or input
404
Requested resource does not exist
409
Conflicting resource state, if applicable
500
Unexpected server error

Use a consistent JSON error response:
{
  "timestamp": "2026-11-07T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "numDays must be greater than or equal to 0",
  "path": "/v1/date/add"
}
Create an ApiError DTO or Java record for this response.
Implement a global exception handler using @RestControllerAdvice.
Handle at least:
MethodArgumentNotValidException
HttpMessageNotReadableException
ResourceNotFoundException
Unexpected exceptions
Do not expose stack traces, SQL statements, or internal implementation details in API responses. Log unexpected exceptions for debugging.
9. Testing Strategy
Use JUnit 5.
Prioritize unit tests for the business logic before writing extensive integration tests.
Date calculation tests
Calendar-day cases:
Adding positive days.
Adding zero days.
Adding days across a month boundary.
Adding days across a year boundary.
Adding days across leap day.
Business-day cases:
Starting on a Friday.
Crossing a weekend.
Crossing a holiday.
Crossing multiple holidays.
Starting on a weekend.
Adding zero business days.
Consecutive holidays and weekends.
Holidays falling on weekends.
Holiday service cases:
Successfully adding a holiday.
Retrieving holidays for a year.
Updating an existing holiday.
Deleting an existing holiday.
Requesting an unknown holiday ID.
Multiple holidays sharing the same date.
Also test invalid input and exception handling where appropriate.
Tests should verify behavior, not simply repeat implementation details.

10. API Spec
All endpoints use the /v1 prefix. All dates use ISO 8601 (YYYY-MM-DD). All bodies are JSON.

Holiday endpoints

GET /v1/holidays?year={year}
Description: Get all holidays, optionally filtered by year. Omitting year returns all holidays.
Response: 200 OK
[
  { "id": 1, "name": "Christmas", "date": "2027-12-25" },
  ...
]
Errors: 400 if year is not a valid integer.

GET /v1/holidays/{id}
Description: Get one holiday.
Response: 200 OK
{ "id": 1, "name": "Christmas", "date": "2027-12-25" }
Errors: 400 if id is not a number. 404 if no holiday has that id.

POST /v1/holidays
Description: Add a holiday to the calendar. The server generates the id.
Request:
{ "name": "Christmas", "date": "2027-12-25" }
Response: 201 Created, with a Location header of /v1/holidays/{id}
{ "id": 1, "name": "Christmas", "date": "2027-12-25" }
Errors: 400 if name or date is missing, blank, or the date is malformed. 409 if a holiday with the same date and name already exists.

PATCH /v1/holidays/{id}
Description: Partially update a holiday. Only supplied fields change.
Request:
{ "name": "Christmas 2" }
Response: 200 OK
{ "id": 1, "name": "Christmas 2", "date": "2027-12-25" }
Errors: 400 if the date is malformed or name is blank. 404 if no holiday has that id. 409 if the update would create a duplicate (same date and name).

DELETE /v1/holidays/{id}
Description: Remove a holiday from the calendar.
Response: 204 No Content (empty body)
Errors: 404 if no holiday has that id.

Date calculation endpoints

POST /v1/date/add
Description: Add calendar or business days to a date.
Request:
{ "startDate": "2026-11-07", "numDays": 10, "calcType": "CALENDAR" }
Response: 200 OK
{ "startDate": "2026-11-07", "resultDate": "2026-11-17", "numDays": 10, "calcType": "CALENDAR" }
Errors: 400 if startDate is missing or malformed, numDays is missing or outside 0..100000, or calcType is missing or not CALENDAR/BUSINESS.

POST /v1/date/subtract
Description: Subtract calendar or business days from a date. Same request/response shape as add.
Request:
{ "startDate": "2026-11-17", "numDays": 10, "calcType": "CALENDAR" }
Response: 200 OK
{ "startDate": "2026-11-17", "resultDate": "2026-11-07", "numDays": 10, "calcType": "CALENDAR" }
Errors: same as add.

POST /v1/date/difference
Description: Count calendar days and business days in the interval (startDate, endDate].
Request:
{ "startDate": "2026-11-07", "endDate": "2026-11-17" }
Response: 200 OK
{ "startDate": "2026-11-07", "endDate": "2026-11-17", "calendarDays": 10, "businessDays": 7 }
Errors: 400 if either date is missing or malformed, or if startDate is after endDate (reversed dates are rejected).

Standardized Error:
{
  "timestamp": "2026-11-07T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "numDays must be greater than or equal to 0",
  "path": "/v1/date/add"
}

Assumptions:
All dates use ISO 8601 (YYYY-MM-DD).
Multiple holidays may share a date, but exact duplicates (same date and name) are rejected.
Duplicate dates are counted only once when calculating business days.
Starting dates are excluded when adding or subtracting days.
Business days exclude Saturday, Sunday, and configured holidays.
Difference calculations exclude the starting date and include the ending date.
Adding or subtracting zero days returns the original date.
startDate equal to endDate is allowed for difference and returns 0 for both counts.



11. Development Guidelines for Claude
Act as a collaborative programming tutor and development assistant.
The goal is not just to generate a working application. The developer should understand the code, architecture, and design decisions well enough to explain them during the assessment.
Follow these guidelines:
Explain before implementing
Explain the purpose of a new class, method, annotation, or dependency before introducing it.
Explain how each component fits into the overall architecture.
When relevant, explain why a particular approach was selected over alternatives.
Work incrementally
Build one logical feature at a time.
Do not generate the entire application in one response.
Inspect the existing repository and follow its current structure before making changes.
Avoid overwriting or restructuring working code without explaining why.
Run relevant tests after meaningful changes.
Teach the underlying concepts
When introducing unfamiliar Java or Spring concepts, explain them in straightforward terms.
Pay particular attention to:
Dependency injection.
Spring annotations such as @RestController, @Service, and @Repository.
DTOs versus entities.
Interfaces and repository abstractions.
JPA and Hibernate.
Exception handling.
Bean validation.
Unit testing and mocking.
HTTP methods and REST conventions.
Use small examples when they help explain a concept.
Preserve simplicity
Prefer readable, straightforward Java.
Avoid unnecessary design patterns and abstractions.
Do not introduce technologies that are not required.
Avoid premature optimization.
Do not add features beyond the assessment without discussing their value first.
Be transparent
Do not silently make significant architectural decisions.
Point out assumptions and potential edge cases.
Explain any tradeoffs.
Do not claim tests pass unless they have actually been run.
If an error occurs, explain its cause and walk through the fix.
12. Initial Implementation Roadmap
Follow this general order:
Inspect the repository and understand the assignment.
Review the Maven dependencies and application configuration.
Set up and verify the file-based H2 database.
Implement the Holiday entity and repository.
Implement holiday-management service and controller.
Write tests for holiday management.
Implement calendar-day calculations.
Implement business-day calculations using the holiday repository.
Write comprehensive date-calculation tests.
Implement request validation and global exception handling.
Start by inspecting the existing project before changing anything. When given the directive to move to the next step, consider if there is anything that should be done before that, and ask for confirmation before writing anything.

Primary principle: Build a small, reliable, well-tested application that demonstrates a strong understanding of Java, Spring Boot, REST API design, database persistence, and software engineering fundamentals.

