# Aufgabe 4a - invoice-service extrahieren

## Neuen Service erstellen

Nutze den [Sprint Initializr](https://start.spring.io) um einen neuen Service `invoice-service` zu erstellen.

## REST-Controller erstellen

```java
package hotel.invoice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Invoice Service API
 * Exposes invoice management endpoints for inter-service communication
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceServiceApi invoiceServiceApi;
    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceServiceApi invoiceServiceApi, InvoiceRepository invoiceRepository) {
        this.invoiceServiceApi = invoiceServiceApi;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Create a new invoice
     * Called by Payment Service via REST adapter
     *
     * @param request contains customer name, total amount, and line items
     * @return created invoice with ID
     */
    @PostMapping
    public ResponseEntity<Long> createInvoice(@RequestBody InvoiceRequest request) {
        try {
            Long invoiceId = invoiceServiceApi.createInvoice(
                request.getCustomerName(),
                request.getTotalAmount(),
                request.getLineItemsPerRoom()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(invoiceId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get invoice by ID
     *
     * @param invoiceId the invoice ID
     * @return the invoice details
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceDetailsResponse> getInvoice(@PathVariable Long invoiceId) {
        Optional<Invoice> invoice = invoiceRepository.findById(invoiceId);
        if (invoice.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Invoice inv = invoice.get();
        return ResponseEntity.ok(new InvoiceDetailsResponse(
            inv.getId(),
            inv.getCustomerName(),
            inv.getTotalAmount(),
            inv.getRoomBookings().values().stream()
                .map(rb -> new RoomBookingDetail(rb.getRoomNumber(), rb.getLineItems().size()))
                .toList()
        ));
    }

    /**
     * Delete all invoices (for testing)
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        invoiceServiceApi.deleteAll();
        return ResponseEntity.noContent().build();
    }

    /**
     * DTO for invoice creation request
     */
    public static class InvoiceRequest {
        private String customerName;
        private double totalAmount;
        private Map<String, List<String>> lineItemsPerRoom;

        public InvoiceRequest() {
        }

        public InvoiceRequest(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.lineItemsPerRoom = lineItemsPerRoom;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Map<String, List<String>> getLineItemsPerRoom() {
            return lineItemsPerRoom;
        }

        public void setLineItemsPerRoom(Map<String, List<String>> lineItemsPerRoom) {
            this.lineItemsPerRoom = lineItemsPerRoom;
        }
    }

    /**
     * DTO for invoice creation response
     */
    public static class InvoiceResponse {
        private Long invoiceId;
        private String customerName;
        private double totalAmount;

        public InvoiceResponse() {
        }

        public InvoiceResponse(Long invoiceId, String customerName, double totalAmount) {
            this.invoiceId = invoiceId;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
        }

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    /**
     * DTO for invoice details response
     */
    public static class InvoiceDetailsResponse {
        private Long invoiceId;
        private String customerName;
        private double totalAmount;
        private List<RoomBookingDetail> roomBookings;

        public InvoiceDetailsResponse() {
        }

        public InvoiceDetailsResponse(Long invoiceId, String customerName, double totalAmount, List<RoomBookingDetail> roomBookings) {
            this.invoiceId = invoiceId;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.roomBookings = roomBookings;
        }

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public List<RoomBookingDetail> getRoomBookings() {
            return roomBookings;
        }

        public void setRoomBookings(List<RoomBookingDetail> roomBookings) {
            this.roomBookings = roomBookings;
        }
    }

    /**
     * DTO for room booking detail in invoice
     */
    public static class RoomBookingDetail {
        private String roomNumber;
        private int lineItemCount;

        public RoomBookingDetail() {
        }

        public RoomBookingDetail(String roomNumber, int lineItemCount) {
            this.roomNumber = roomNumber;
            this.lineItemCount = lineItemCount;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        public int getLineItemCount() {
            return lineItemCount;
        }

        public void setLineItemCount(int lineItemCount) {
            this.lineItemCount = lineItemCount;
        }
    }
}
```

## REST-Schnittstelle aufrufen

### Erstellen eines Ports
```java
package hotel.payment;

import java.util.List;
import java.util.Map;

/**
 * Port interface for invoice service operations.
 * Defines the contract for the payment module to interact with the invoice service.
 * This allows the implementation to be swapped (e.g., from direct calls to REST calls)
 * without changing the payment module code.
 *
 * The port interface decouples the payment module from the specific implementation
 * of invoice generation, enabling service decomposition and independent evolution.
 */
public interface InvoiceServicePort {

    /**
     * Creates an invoice for a customer's bookings.
     * This method is called by the payment service after validating payment availability.
     *
     * The lineItemsPerRoom map contains descriptions of bookings for each room.
     * A unique invoice ID is returned for reference and tracking.
     *
     * @param customerName the name of the customer
     * @param totalAmount the total amount to be invoiced
     * @param lineItemsPerRoom a map of room numbers to line item descriptions (dates booked, etc.)
     * @return the ID of the created invoice
     * @throws IllegalArgumentException if the parameters are invalid
     */
    Long createInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom);
}
```

### Implementierung des Adapters

```java
package hotel.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * REST adapter implementation of InvoiceServicePort.
 * Uses RestTemplate to make HTTP calls to the invoice service.
 * <p>
 * This adapter allows the payment module to communicate with the invoice service
 * without direct coupling. When services are split into separate applications,
 * this adapter will make actual HTTP calls to the invoice service.
 */
@Service
public class PaymentInvoiceServiceRestAdapter implements InvoiceServicePort {

    private static final Logger logger = LoggerFactory.getLogger(PaymentInvoiceServiceRestAdapter.class);

    private final RestTemplate restTemplate;
    private final String invoiceServiceUrl;

    /**
     * Constructor that injects the RestTemplate and invoice service URL.
     *
     * @param restTemplate      the RestTemplate bean for making HTTP calls
     * @param invoiceServiceUrl the base URL of the invoice service
     */
    public PaymentInvoiceServiceRestAdapter(RestTemplate restTemplate,
                                            @Value("${invoice-service.url}") String invoiceServiceUrl) {
        this.restTemplate = restTemplate;
        this.invoiceServiceUrl = invoiceServiceUrl;
    }

    /**
     * Creates an invoice for a customer's bookings.
     * Makes a REST call to the invoice service endpoint.
     *
     * @param customerName     the name of the customer
     * @param totalAmount      the total amount to be invoiced
     * @param lineItemsPerRoom a map of room numbers to line item descriptions
     * @return the ID of the created invoice
     * @throws IllegalArgumentException if the parameters are invalid
     */
    @Override
    public Long createInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {
        logger.info("Creating invoice via REST adapter: customer={}, amount={}", customerName, totalAmount);

        try {
            String url = invoiceServiceUrl + "/api/invoices";
            logger.debug("Calling invoice service: {}", url);

            // Create request body
            InvoiceRequest request = new InvoiceRequest(customerName, totalAmount, lineItemsPerRoom);

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<InvoiceRequest> entity = new HttpEntity<>(request, headers);

            // Make REST call to invoice service
            // The endpoint returns just the Long invoiceId
            Long invoiceId = restTemplate.postForObject(url, entity, Long.class);

            if (invoiceId == null || invoiceId <= 0) {
                throw new IllegalArgumentException("Invoice service did not return a valid invoice ID");
            }

            logger.info("Invoice created successfully: customer={}, invoiceId={}", customerName, invoiceId);
            return invoiceId;

        } catch (HttpClientErrorException e) {
            logger.error("Client error calling invoice service (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            throw new IllegalArgumentException("Failed to create invoice: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling invoice service (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            throw new IllegalArgumentException("Invoice service error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error creating invoice: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to create invoice: " + e.getMessage(), e);
        }
    }

    /**
     * DTO for invoice request.
     * Used when making REST calls to the invoice service.
     */
    public static class InvoiceRequest {
        public String customerName;
        public double totalAmount;
        public Map<String, List<String>> lineItemsPerRoom;

        public InvoiceRequest() {
        }

        public InvoiceRequest(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.lineItemsPerRoom = lineItemsPerRoom;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Map<String, List<String>> getLineItemsPerRoom() {
            return lineItemsPerRoom;
        }

        public void setLineItemsPerRoom(Map<String, List<String>> lineItemsPerRoom) {
            this.lineItemsPerRoom = lineItemsPerRoom;
        }
    }

    /**
     * DTO for invoice response.
     * Used when processing responses from the invoice service.
     */
    public static class InvoiceResponse {
        public Long invoiceId;
        public String message;

        public InvoiceResponse() {
        }

        public InvoiceResponse(Long invoiceId, String message) {
            this.invoiceId = invoiceId;
            this.message = message;
        }

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
```

## Properties des invoice-service anpassen
```
server:
  port: 8083

spring:
  application:
    name: invoice-service
  # H2 In-Memory
  datasource:
    url: jdbc:h2:mem:testdb
    driverClassName: org.h2.Driver
    username: sa
    password:
  # Hibernate DDL Strategie
  jpa:
    hibernate:
      ddl-auto: update
  # SQL-Ausgabe in Console
    show-sql: true
  # H2-Konsole aktivieren (im Browser erreichbar unter http://localhost:8083/h2-console)
  h2:
    console:
      enabled: true
```