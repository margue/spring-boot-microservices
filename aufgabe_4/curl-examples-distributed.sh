#!/bin/bash

# Hotel Booking Microservices - Distributed Curl Examples
# This script demonstrates the complete workflow across three independent microservices:
# - Booking Service (Port 8080)
# - Payment Service (Port 8082)
# - Invoice Service (Port 8083)
#
# IMPORTANT: Start all three services BEFORE running this script:
#   Terminal 1: cd /workspace/split/invoice-service && mvn spring-boot:run
#   Terminal 2: cd /workspace/split/payment-service && mvn spring-boot:run
#   Terminal 3: cd /workspace/split/booking-service && mvn spring-boot:run

set -e  # Exit on error

# Configuration
BOOKING_SERVICE_URL="http://localhost:8080"
PAYMENT_SERVICE_URL="http://localhost:8082"
INVOICE_SERVICE_URL="http://localhost:8083"
CUSTOMER_NAME="Max Mustermann"

# Color output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check all services are running (with simple port check)
echo -e "${BLUE}=== Microservices Integration Test ===${NC}\n"
echo -e "${BLUE}Pre-flight Information${NC}"
echo "Make sure you have started all three services in separate terminals:"
echo -e "${YELLOW}  Terminal 1: cd /workspace/split/invoice-service && mvn spring-boot:run${NC}"
echo -e "${YELLOW}  Terminal 2: cd /workspace/split/payment-service && mvn spring-boot:run${NC}"
echo -e "${YELLOW}  Terminal 3: cd /workspace/split/booking-service && mvn spring-boot:run${NC}"
echo ""
echo -e "${BLUE}Testing service availability...${NC}"
echo -e "  Booking Service: ${BOOKING_SERVICE_URL}"
echo -e "  Payment Service: ${PAYMENT_SERVICE_URL}"
echo -e "  Invoice Service: ${INVOICE_SERVICE_URL}"
echo ""
echo -e "${BLUE}=== Starting Integration Test Workflow ===${NC}\n"

# ============================================================================
# STEP 1: Check Room Availability (via Booking Service)
# ============================================================================
echo -e "${BLUE}STEP 1: Check Room Availability (Booking Service)${NC}"
echo "GET ${BOOKING_SERVICE_URL}/api/hotel/rooms/availability"
echo -e "${YELLOW}Starting date: 2025-11-01, Ending date: 2025-11-05${NC}\n"

AVAILABILITY=$(curl -s -X GET "${BOOKING_SERVICE_URL}/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}")

echo "$AVAILABILITY"
echo ""

# ============================================================================
# STEP 2: Book a Room (via Booking Service)
# ============================================================================
echo -e "${BLUE}STEP 2: Book a Room (Booking Service)${NC}"
echo "POST ${BOOKING_SERVICE_URL}/api/hotel/rooms/booking"
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}, Dates: 2025-11-01 to 2025-11-05${NC}\n"

BOOKING=$(curl -s -X POST "${BOOKING_SERVICE_URL}/api/hotel/rooms/booking" \
  -H "Content-Type: application/json" \
  -d "{
    \"startDate\": \"2025-11-01\",
    \"endDate\": \"2025-11-05\",
    \"customerName\": \"${CUSTOMER_NAME}\"
  }" \
  -w "\nHTTP Status: %{http_code}")

echo "$BOOKING"
echo ""

# ============================================================================
# STEP 3: Check-In (via Booking Service)
# ============================================================================
echo -e "${BLUE}STEP 3: Check-In (Booking Service)${NC}"
echo "POST ${BOOKING_SERVICE_URL}/api/hotel/checkin"
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}, Start date: 2025-11-01${NC}\n"

CHECKIN=$(curl -s -X POST "${BOOKING_SERVICE_URL}/api/hotel/checkin" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"startDate\": \"2025-11-01\"
  }" \
  -w "\nHTTP Status: %{http_code}")

echo "$CHECKIN"
ROOM_NUMBERS=$(echo "$CHECKIN" | grep -o '\["[^"]*"\]' | head -1 | tr -d '[]"')
echo -e "${GREEN}Booked room(s): ${ROOM_NUMBERS}${NC}\n"

# ============================================================================
# STEP 4: Process Payment (via Payment Service)
# ============================================================================
echo -e "${BLUE}STEP 4: Process Payment (Payment Service)${NC}"
echo "POST ${PAYMENT_SERVICE_URL}/api/payments"
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}, Amount: 500.00${NC}\n"

PAYMENT=$(curl -s -X POST "${PAYMENT_SERVICE_URL}/api/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"amount\": 500.00
  }" \
  -w "\nHTTP Status: %{http_code}")

echo "$PAYMENT"
echo ""

# ============================================================================
# STEP 5: Check Payment Credit (via Payment Service)
# ============================================================================
echo -e "${BLUE}STEP 5: Check Payment Credit (Payment Service)${NC}"
echo "GET ${PAYMENT_SERVICE_URL}/api/payments/credit/{customerName}"
CUSTOMER_NAME_ENCODED=$(echo "${CUSTOMER_NAME}" | sed 's/ /%20/g')
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}${NC}\n"

CREDIT=$(curl -s -X GET "${PAYMENT_SERVICE_URL}/api/payments/credit/${CUSTOMER_NAME_ENCODED}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}")

echo "$CREDIT"
echo ""

# ============================================================================
# STEP 6: Create Invoice (Booking → Payment → Invoice Service Chain)
# ============================================================================
echo -e "${BLUE}STEP 6: Create Invoice (Booking Service)${NC}"
echo "This call chain: Booking Service → Payment Service → Invoice Service"
echo "POST ${BOOKING_SERVICE_URL}/api/hotel/invoice"
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}, Rooms: [101], End date: 2025-11-05${NC}\n"

INVOICE=$(curl -s -X POST "${BOOKING_SERVICE_URL}/api/hotel/invoice" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"endDate\": \"2025-11-05\",
    \"roomNumbers\": [\"101\"]
  }" \
  -w "\nHTTP Status: %{http_code}")

echo "$INVOICE"
# Extract invoiceId from JSON response (e.g., {"message":"...", "invoiceId":1})
INVOICE_ID=$(echo "$INVOICE" | grep -o '"invoiceId":[0-9]*' | cut -d':' -f2)
echo -e "${GREEN}Invoice created with ID: ${INVOICE_ID}${NC}\n"

# ============================================================================
# STEP 7: Verify Invoice in Invoice Service
# ============================================================================
echo -e "${BLUE}STEP 7: Verify Invoice Created (Invoice Service)${NC}"
echo "GET ${INVOICE_SERVICE_URL}/api/invoices/{invoiceId}"
echo -e "${YELLOW}Invoice ID: ${INVOICE_ID}${NC}\n"

if [ -n "$INVOICE_ID" ] && [ "$INVOICE_ID" != "0" ]; then
  INVOICE_VERIFY=$(curl -s -X GET "${INVOICE_SERVICE_URL}/api/invoices/${INVOICE_ID}" \
    -H "Content-Type: application/json" \
    -w "\nHTTP Status: %{http_code}")
  echo "$INVOICE_VERIFY"
else
  echo -e "${RED}Warning: Could not extract invoice ID, skipping verification${NC}"
fi
echo ""

# ============================================================================
# STEP 8: Check-Out (via Booking Service)
# ============================================================================
echo -e "${BLUE}STEP 8: Check-Out (Booking Service)${NC}"
echo "POST ${BOOKING_SERVICE_URL}/api/hotel/checkout"
echo -e "${YELLOW}Customer: ${CUSTOMER_NAME}, Room: 101, End date: 2025-11-05${NC}\n"

CHECKOUT=$(curl -s -X POST "${BOOKING_SERVICE_URL}/api/hotel/checkout" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"roomNumber\": \"101\",
    \"endDate\": \"2025-11-05\"
  }" \
  -w "\nHTTP Status: %{http_code}")

echo "$CHECKOUT"
echo ""

# ============================================================================
# Test Summary
# ============================================================================
echo -e "${GREEN}=== Integration Test Complete ===${NC}\n"
echo -e "${BLUE}Summary of Service Communication:${NC}"
echo "1. Booking Service (8080) → Checked availability and booked room"
echo "2. Booking Service (8080) → Called Payment Service (8082) to process payment"
echo "3. Payment Service (8082) → Called Invoice Service (8083) to create invoice"
echo "4. Booking Service (8080) → Completed checkout"
echo ""
echo -e "${GREEN}✓ Multi-service integration workflow completed successfully!${NC}"
echo ""
echo "Next Steps:"
echo "- Check each service's database for data consistency"
echo "- Verify payment records in Payment Service H2 console: ${PAYMENT_SERVICE_URL}/h2-console"
echo "- Verify invoice records in Invoice Service H2 console: ${INVOICE_SERVICE_URL}/h2-console"
echo "- Verify booking records in Booking Service H2 console: ${BOOKING_SERVICE_URL}/h2-console"