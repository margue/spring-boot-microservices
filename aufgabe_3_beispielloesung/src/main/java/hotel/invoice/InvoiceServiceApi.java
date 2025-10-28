package hotel.invoice;

import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.Map;

@NamedInterface(name = "invoiceServiceApi")
public interface InvoiceServiceApi {
    Long createInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemDescriptionsPerRoom);
    void deleteAll();
}
