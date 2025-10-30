package hotel.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerName(String customerName);
}