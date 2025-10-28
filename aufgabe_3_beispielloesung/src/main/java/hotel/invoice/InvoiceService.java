package hotel.invoice;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class InvoiceService implements InvoiceServiceApi {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public Long createInvoice(String customerName, double totalAmount, Map<String, List<String>> lineItemDescriptionsPerRoom) {
        Map<String, List<LineItem>> lineItemsPerRoom = lineItemDescriptionsPerRoom.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(LineItem::new).toList())
                );

        Invoice invoice = new Invoice(customerName, lineItemsPerRoom, totalAmount);
        invoice = invoiceRepository.save(invoice);
        return invoice.getId();
    }

    @Override
    public void deleteAll() {
        invoiceRepository.deleteAll();
    }
}
