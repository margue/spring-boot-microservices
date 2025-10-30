package hotel.payment;

import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.Map;

@NamedInterface(name = "paymentServiceApi")
public interface PaymentServiceApi {
    Long produceInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemsPerRoom);
    void deleteAll();
}
