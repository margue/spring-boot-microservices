#!/bin/bash

# Hotel Booking Service - Comprehensive Curl Examples
# This script demonstrates all available API endpoints with executable curl commands

set -e  # Exit on error

# Configuration
BASE_URL="http://localhost:8080"
CUSTOMER_NAME="Max Mustermann"

# Color output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Hotel Booking Service - API Examples ===${NC}\n"

# ============================================================================
# 1. CHECK ROOM AVAILABILITY
# ============================================================================
echo -e "${BLUE}1. CHECK ROOM AVAILABILITY${NC}"
echo "GET /api/hotel/rooms/availability"
echo -e "${YELLOW}curl -X GET \"${BASE_URL}/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05\"${NC}\n"

curl -X GET "${BASE_URL}/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 2. BOOK A ROOM
# ============================================================================
echo -e "${BLUE}2. BOOK A ROOM${NC}"
echo "POST /api/hotel/rooms/booking"
echo -e "${YELLOW}curl -X POST \"${BASE_URL}/api/hotel/rooms/booking\" \\${NC}"
echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
echo -e "${YELLOW}  -d '{ \"startDate\": \"2025-11-01\", \"endDate\": \"2025-11-05\", \"customerName\": \"${CUSTOMER_NAME}\" }'${NC}\n"

curl -X POST "${BASE_URL}/api/hotel/rooms/booking" \
  -H "Content-Type: application/json" \
  -d "{
    \"startDate\": \"2025-11-01\",
    \"endDate\": \"2025-11-05\",
    \"customerName\": \"${CUSTOMER_NAME}\"
  }" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 3. CHECK-IN
# ============================================================================
echo -e "${BLUE}3. CHECK-IN${NC}"
echo "POST /api/hotel/checkin"
echo -e "${YELLOW}curl -X POST \"${BASE_URL}/api/hotel/checkin\" \\${NC}"
echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
echo -e "${YELLOW}  -d '{ \"customerName\": \"${CUSTOMER_NAME}\", \"startDate\": \"2025-11-01\" }'${NC}\n"

curl -X POST "${BASE_URL}/api/hotel/checkin" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"startDate\": \"2025-11-01\"
  }" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 4. PROCESS PAYMENT
# ============================================================================
echo -e "${BLUE}4. PROCESS PAYMENT${NC}"
echo "POST /api/payments"
echo -e "${YELLOW}curl -X POST \"${BASE_URL}/api/payments\" \\${NC}"
echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
echo -e "${YELLOW}  -d '{ \"customerName\": \"${CUSTOMER_NAME}\", \"amount\": 500.00 }'${NC}\n"

curl -X POST "${BASE_URL}/api/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"amount\": 500.00
  }" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 5. GET REMAINING CREDIT
# ============================================================================
echo -e "${BLUE}5. GET REMAINING CREDIT${NC}"
echo "GET /api/payments/credit/{customerName}"
CUSTOMER_NAME_ENCODED=$(echo "${CUSTOMER_NAME}" | sed 's/ /%20/g')
echo -e "${YELLOW}curl -X GET \"${BASE_URL}/api/payments/credit/${CUSTOMER_NAME_ENCODED}\"${NC}\n"

curl -X GET "${BASE_URL}/api/payments/credit/${CUSTOMER_NAME_ENCODED}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 6. CREATE INVOICE
# ============================================================================
echo -e "${BLUE}6. CREATE INVOICE${NC}"
echo "POST /api/hotel/invoice"
echo -e "${YELLOW}curl -X POST \"${BASE_URL}/api/hotel/invoice\" \\${NC}"
echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
echo -e "${YELLOW}  -d '{ \"customerName\": \"${CUSTOMER_NAME}\", \"endDate\": \"2025-11-05\", \"roomNumbers\": [\"101\"] }'${NC}\n"

curl -X POST "${BASE_URL}/api/hotel/invoice" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"endDate\": \"2025-11-05\",
    \"roomNumbers\": [\"101\"]
  }" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 7. GET REMAINING CREDIT AFTER PAYMENT
# ============================================================================
echo -e "${BLUE}7. GET REMAINING CREDIT AFTER PAYMENT${NC}"
echo "GET /api/payments/credit/{customerName}"
echo -e "${YELLOW}curl -X GET \"${BASE_URL}/api/payments/credit/${CUSTOMER_NAME_ENCODED}\"${NC}\n"

curl -X GET "${BASE_URL}/api/payments/credit/${CUSTOMER_NAME_ENCODED}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# ============================================================================
# 8. CHECK-OUT
# ============================================================================
echo -e "${BLUE}8. CHECK-OUT (After invoice created)${NC}"
echo "POST /api/hotel/checkout"
echo -e "${YELLOW}curl -X POST \"${BASE_URL}/api/hotel/checkout\" \\${NC}"
echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
echo -e "${YELLOW}  -d '{ \"customerName\": \"${CUSTOMER_NAME}\", \"roomNumber\": \"101\", \"endDate\": \"2025-11-05\" }'${NC}\n"

curl -X POST "${BASE_URL}/api/hotel/checkout" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"${CUSTOMER_NAME}\",
    \"roomNumber\": \"101\",
    \"endDate\": \"2025-11-05\"
  }" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo -e "${GREEN}=== All examples completed ===${NC}\n"
echo "For more details, see CURL_REFERENCE.md"