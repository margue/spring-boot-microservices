package hotel.hotel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Component responsible for initializing default rooms on application startup.
 * This component runs automatically when the Spring application context is initialized.
 */
@Component
public class RoomInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RoomInitializer.class);

    private final RoomRepository roomRepository;

    public RoomInitializer(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Initializes three default rooms on application startup if they don't already exist.
     *
     * @param args The application arguments (not used)
     * @throws Exception if an error occurs during initialization
     */
    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        logger.info("Starting RoomInitializer...");

        // Initialize Room 101 if not exists
        if (roomRepository.findByRoomNumber("101") == null) {
            Room room101 = new Room("101", null);
            roomRepository.save(room101);
            logger.info("Created Room 101");
        }

        // Initialize Room 102 if not exists
        if (roomRepository.findByRoomNumber("102") == null) {
            Room room102 = new Room("102", null);
            roomRepository.save(room102);
            logger.info("Created Room 102");
        }

        // Initialize Room 103 if not exists
        if (roomRepository.findByRoomNumber("103") == null) {
            Room room103 = new Room("103", null);
            roomRepository.save(room103);
            logger.info("Created Room 103");
        }

        long totalRooms = roomRepository.count();
        logger.info("Room initialization completed. Total rooms in database: {}", totalRooms);
    }
}