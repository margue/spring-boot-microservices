package hotel.hotel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
interface RoomRepository extends JpaRepository<Room, String> {

    /**
     * Findet alle Räume, die Buchungen für einen bestimmten Kunden haben.
     * 
     * @param customerName Name des Kunden
     * @return Liste der Räume mit Buchungen des Kunden
     */
    @Query("SELECT DISTINCT r FROM Room r JOIN r.bookings b WHERE b.customerName = :customerName")
    List<Room> findAllRoomsWithBookingIntervalsByCustomerName(@Param("customerName") String customerName);

    /**
     * Findet alle Buchungsintervalle für einen bestimmten Kunden.
     * 
     * @param customerName Name des Kunden
     * @return Liste der Buchungsintervalle des Kunden
     */
    @Query("SELECT b FROM Room r JOIN r.bookings b WHERE b.customerName = :customerName")
    List<BookingInterval> findAllBookingIntervalsByCustomerName(@Param("customerName") String customerName);

    default void markBookingsAsInvoiced(Map<String, List<BookingInterval>> bookingsForRooms, Long invoiceId){
        bookingsForRooms.keySet().forEach(roomNumber -> {
            Room room = findByRoomNumber(roomNumber);
            room.getBookings().forEach(booking -> {
                if (listContainsBooking(bookingsForRooms.get(roomNumber), booking)) {
                    booking.setInvoiceId(invoiceId);
                }
            });
            save(room);
        });
    }

    Room findByRoomNumber(String roomNumber);

    private boolean listContainsBooking(List<BookingInterval> bookingIntervals, BookingInterval booking) {
        for (BookingInterval bookingInterval : bookingIntervals) {
            if (bookingInterval.getCustomerName().equals(booking.getCustomerName()) && bookingInterval.getStartDate().equals(booking.getStartDate())) {
                return true;
            }
        }
        return false;
    }
}
