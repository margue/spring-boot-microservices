Hilfreiche Links und Snippets für Aufgabe 3

# Definition von expliziten Appikationsmodulen

Link zur Dokumentation: https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.explicit-dependencies

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Hotel",
        allowedDependencies = {
                "payment",
                "invoice",
                "shared"})
package hotel.hotel;
```
# Aufgabe 3a: Auflösen der Abhängigkeitsverletzungen

- Invoices sollten keine BookingIntervals kennen
- Methode produceInvoice im PaymentService enthält Hotellogik
- Preisberechnung im HotelController

# Aufgabe 3b: Named Interfaces implementieren

Link zur Dokumentation: https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.named-interfaces

## Definition von Named Interfaces
```java
package hotel.payment;

import org.springframework.modulith.NamedInterface;

(...)

@NamedInterface(name = "paymentServiceApi")
public interface PaymentServiceApi {
    Long produceInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom);
    
    (...)
}
```
## Verwendung von Named Interfaces
```java
@org.springframework.modulith.ApplicationModule(
  displayName = "Hotel", 
  allowedDependencies = { 
    "payment :: paymentServiceApi",
    "shared"})
package hotel.hotel;
```

# Kompilieren und Testen des Projekts

Das Projekt kann mit dem Maven Wrapper kompiliert und getestet werden:

```
./mvnw clean install