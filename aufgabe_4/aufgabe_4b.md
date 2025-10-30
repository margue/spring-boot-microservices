# Aufgabe 4a - invoice-service extrahieren

## Neuen Service erstellen

Erstelle einen neuen Ordner für den `payment-service`.

## REST-Controller erstellen

```java
package hotel.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Invoice endpoints in Payment Service
 * This controller delegates invoice creation to PaymentService.produceInvoice()
 * which handles the complete payment→invoice workflow.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentInvoiceController {

    private final PaymentServiceApi paymentServiceApi;

    public PaymentInvoiceController(PaymentServiceApi paymentServiceApi) {
        this.paymentServiceApi = paymentServiceApi;
    }

    /**
     * Create a new invoice by delegating to PaymentService
     * Called by Booking Service via POST /api/payments/invoice
     * This endpoint receives requests from the Booking Service's REST adapter
     * and delegates invoice creation through the payment service workflow.
     *
     * @param request contains customer name, total amount, and line items
     * @return created invoice with ID
     */
    @PostMapping("/invoice")
    public ResponseEntity<Long> createInvoice(@RequestBody InvoiceRequest request) {
        try {
            Long invoiceId = paymentServiceApi.produceInvoice(
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
}
```

## REST-Schnittstelle aufrufen

### Erstellen eines Ports
```java
package hotel.hotel;

import java.util.List;
import java.util.Map;

/**
 * Port interface for payment service operations.
 * Defines the contract for the booking module to interact with the payment service.
 * This allows the implementation to be swapped (e.g., from direct calls to REST calls)
 * without changing the booking module code.
 *
 * The port interface decouples the booking module from the specific implementation
 * of payment processing, enabling service decomposition and independent evolution.
 */
public interface PaymentServicePort {

    /**
     * Creates an invoice for a customer's booking.
     * Generates an invoice with line items for each room booked.
     *
     * @param customerName the name of the customer
     * @param totalAmount the total amount to be invoiced
     * @param lineItemsPerRoom a map of room numbers to line item descriptions
     * @return the invoice ID generated by the payment service
     */
    Long produceInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom);
}

```

### Implementierung des Adapters

```java
package hotel.hotel;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * REST adapter implementation of PaymentServicePort.
 * Uses RestTemplate to make HTTP calls to the payment service.
 *
 * This adapter allows the booking module to communicate with the payment service
 * without direct coupling. When services are split into separate applications,
 * this adapter will make actual HTTP calls to the payment service.
 */
@Service
public class PaymentServiceRestAdapter implements PaymentServicePort {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceRestAdapter.class);

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    /**
     * Constructor that injects the RestTemplate and payment service URL.
     *
     * @param restTemplate the RestTemplate bean for making HTTP calls
     * @param paymentServiceUrl the base URL of the payment service
     */
    public PaymentServiceRestAdapter(RestTemplate restTemplate,
                                     @Value("${payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    /**
     * Creates an invoice for a customer's booking via REST call to payment service.
     * Generates an invoice with line items for each room booked.
     *
     * @param customerName the name of the customer
     * @param totalAmount the total amount to be invoiced
     * @param lineItemsPerRoom a map of room numbers to line item descriptions
     * @return the invoice ID generated by the payment service
     * @throws IllegalStateException if invoice creation fails
     */
    @Override
    public Long produceInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {
        logger.info("Creating invoice via REST adapter: customer={}, totalAmount={}", customerName, totalAmount);

        try {
            String url = paymentServiceUrl + "/api/payments/invoice";
            logger.debug("Calling payment service: {}", url);

            // Create request body
            PaymentInvoiceRequest request = new PaymentInvoiceRequest(customerName, totalAmount, lineItemsPerRoom);

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<PaymentInvoiceRequest> entity = new HttpEntity<>(request, headers);

            // Make REST call to payment service
            // The endpoint returns the Long invoiceId directly
            Object response = restTemplate.postForObject(url, entity, Object.class);

            Long invoiceId = null;
            if (response instanceof Integer) {
                invoiceId = ((Integer) response).longValue();
            } else if (response instanceof Long) {
                invoiceId = (Long) response;
            } else if (response != null) {
                invoiceId = Long.valueOf(response.toString());
            }

            if (invoiceId == null || invoiceId <= 0) {
                throw new IllegalStateException("Payment service returned null or invalid invoice ID");
            }

            logger.info("Invoice created successfully: customer={}, invoiceId={}", customerName, invoiceId);
            return invoiceId;

        } catch (HttpClientErrorException e) {
            logger.error("Client error calling payment service (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("Failed to create invoice: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("Server error calling payment service (HTTP {}): {}", e.getStatusCode(), e.getMessage());
            throw new IllegalStateException("Payment service error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error creating invoice: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to create invoice: " + e.getMessage(), e);
        }
    }

    /**
     * DTO for payment/invoice request.
     * Used when making REST calls to the payment service's invoice endpoint.
     */
    public static class PaymentInvoiceRequest {
        @JsonProperty("customerName")
        public String customerName;

        @JsonProperty("totalAmount")
        public double totalAmount;

        @JsonProperty("lineItemsPerRoom")
        public Map<String, List<String>> lineItemsPerRoom;

        public PaymentInvoiceRequest() {
        }

        public PaymentInvoiceRequest(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom) {
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
     * Returned from the payment service.
     */
    public static class InvoiceResponse {
        public Long invoiceId;

        public InvoiceResponse() {
        }

        public InvoiceResponse(Long invoiceId) {
            this.invoiceId = invoiceId;
        }

        public Long getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(Long invoiceId) {
            this.invoiceId = invoiceId;
        }
    }
}
```

## Properties des payment-service anpassen
```
server:
  port: 8082

spring:
  application:
    name: payment-service
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
  # H2-Konsole aktivieren (im Browser erreichbar unter http://localhost:8082/h2-console)
  h2:
    console:
      enabled: true

# Service URLs for inter-service communication
# These URLs are used by REST adapters to call other microservices
invoice-service:
  url: http://localhost:8083
```