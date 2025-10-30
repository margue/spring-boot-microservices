package hotel.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
