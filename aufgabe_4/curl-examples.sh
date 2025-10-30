#!/bin/bash

# Hotel Booking Service - Comprehensive Curl Examples
# This script demonstrates all available API endpoints with executable curl commands
# Works on macOS, Linux, and Windows (with bash/git-bash)
# Also supports IntelliJ integration and selective request execution
#
# USAGE:
#   Bash/macOS/Linux:
#     bash curl-examples.sh                  # Run all requests
#     bash curl-examples.sh 1                # Run only request 1
#     bash curl-examples.sh 2                # Run only request 2
#     bash curl-examples.sh help             # Show help
#
#   IntelliJ/PyCharm (Run → Edit Configurations → Shell Script):
#     Script path: /path/to/curl-examples.sh
#     Parameters: 1  (or any request number)
#     Working directory: /workspace/split/hotel-booking-service
#
# REQUIREMENTS:
#   - bash 3.0+ (installed by default on macOS/Linux)
#   - curl (installed by default on macOS/Linux, Windows 10+)
#
# TROUBLESHOOTING:
#   Windows: Use git-bash or WSL (Windows Subsystem for Linux)
#   macOS: brew install curl (if needed)
#   Linux: sudo apt-get install curl (Debian/Ubuntu)

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
CUSTOMER_NAME="${CUSTOMER_NAME:-Max Mustermann}"
REQUEST_NUMBER="${1:-0}"  # 0 = run all, 1-8 = run specific request

# Color output (works on macOS, Linux, Windows bash)
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to show help
show_help() {
    cat << 'EOF'
Hotel Booking Service - Curl Examples (Bash Version)

USAGE:
  Run all requests:
    bash curl-examples.sh

  Run specific request:
    bash curl-examples.sh 1    # Run only request 1
    bash curl-examples.sh 2    # Run only request 2

  Show this help:
    bash curl-examples.sh help

AVAILABLE REQUESTS:
  1. CHECK ROOM AVAILABILITY
  2. BOOK A ROOM
  3. CHECK-IN
  4. PROCESS PAYMENT
  5. GET REMAINING CREDIT
  6. CREATE INVOICE
  7. GET REMAINING CREDIT AFTER PAYMENT
  8. CHECK-OUT

ENVIRONMENT VARIABLES:
  BASE_URL: Base URL for the booking service (default: http://localhost:8080)
  CUSTOMER_NAME: Customer name for requests (default: Max Mustermann)

EXAMPLES:
  # Run all requests
  bash curl-examples.sh

  # Run only the booking request
  bash curl-examples.sh 2

  # Set custom base URL
  export BASE_URL="http://booking-service.example.com"
  bash curl-examples.sh 1

TROUBLESHOOTING:
  If you see "curl: command not found":
    - macOS: brew install curl
    - Linux: sudo apt-get install curl
    - Windows: Use git-bash or install curl separately

  If colors don't work on Windows:
    - Using git-bash: Should work automatically
    - Using standard cmd: Download git-bash or WSL

EOF
    exit 0
}

# Check for help flag
if [[ "$REQUEST_NUMBER" == "help" ]] || [[ "$REQUEST_NUMBER" == "-h" ]] || [[ "$REQUEST_NUMBER" == "--help" ]]; then
    show_help
fi

# Function to print separator
print_separator() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
}

# Function to execute a curl request
execute_request() {
    local number=$1
    local title=$2
    local method=$3
    local endpoint=$4
    local data=$5

    echo ""
    echo -e "${BLUE}REQUEST $number : $title${NC}"
    echo -e "${BLUE}$method $endpoint${NC}"
    print_separator

    if [[ "$method" == "GET" ]]; then
        echo -e "${YELLOW}curl -X GET \"${BASE_URL}${endpoint}\" -H \"Content-Type: application/json\"${NC}\n"
        curl -X GET "${BASE_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -w "\nHTTP Status: %{http_code}\n\n"
    else
        echo -e "${YELLOW}curl -X $method \"${BASE_URL}${endpoint}\" \\${NC}"
        echo -e "${YELLOW}  -H \"Content-Type: application/json\" \\${NC}"
        echo -e "${YELLOW}  -d '$data'${NC}\n"
        curl -X "$method" "${BASE_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\nHTTP Status: %{http_code}\n\n"
    fi
}

# Main script
echo ""
print_separator
echo -e "${BLUE}  Hotel Booking Service - API Examples (Bash Version)${NC}"
print_separator
echo ""
echo -e "${BLUE}Base URL: $BASE_URL${NC}"
echo -e "${BLUE}Customer Name: $CUSTOMER_NAME${NC}"
echo ""

# Define requests as arrays
declare -a request_numbers=(1 2 3 4 5 6 7 8)
declare -a request_titles=(
    "CHECK ROOM AVAILABILITY"
    "BOOK A ROOM"
    "CHECK-IN"
    "PROCESS PAYMENT"
    "GET REMAINING CREDIT"
    "CREATE INVOICE"
    "GET REMAINING CREDIT AFTER PAYMENT"
    "CHECK-OUT"
)
declare -a request_methods=("GET" "POST" "POST" "POST" "GET" "POST" "GET" "POST")
declare -a request_endpoints=(
    "/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05"
    "/api/hotel/rooms/booking"
    "/api/hotel/checkin"
    "/api/payments"
    "/api/payments/credit/${CUSTOMER_NAME// /%20}"
    "/api/hotel/invoice"
    "/api/payments/credit/${CUSTOMER_NAME// /%20}"
    "/api/hotel/checkout"
)
declare -a request_data=(
    ""
    "{\"startDate\": \"2025-11-01\", \"endDate\": \"2025-11-05\", \"customerName\": \"${CUSTOMER_NAME}\"}"
    "{\"customerName\": \"${CUSTOMER_NAME}\", \"startDate\": \"2025-11-01\"}"
    "{\"customerName\": \"${CUSTOMER_NAME}\", \"amount\": 500.00}"
    ""
    "{\"customerName\": \"${CUSTOMER_NAME}\", \"endDate\": \"2025-11-05\", \"roomNumbers\": [\"101\"]}"
    ""
    "{\"customerName\": \"${CUSTOMER_NAME}\", \"roomNumber\": \"101\", \"endDate\": \"2025-11-05\"}"
)

# Execute requests
if [[ $REQUEST_NUMBER -eq 0 ]]; then
    # Run all requests
    for i in "${!request_numbers[@]}"; do
        execute_request "${request_numbers[$i]}" "${request_titles[$i]}" "${request_methods[$i]}" "${request_endpoints[$i]}" "${request_data[$i]}"
    done

    echo ""
    print_separator
    echo -e "${GREEN}  ✓ All requests completed!${NC}"
    print_separator
else
    # Run specific request
    if [[ $REQUEST_NUMBER -ge 1 && $REQUEST_NUMBER -le 8 ]]; then
        idx=$((REQUEST_NUMBER - 1))
        execute_request "${request_numbers[$idx]}" "${request_titles[$idx]}" "${request_methods[$idx]}" "${request_endpoints[$idx]}" "${request_data[$idx]}"

        echo ""
        print_separator
        echo -e "${GREEN}  ✓ Request $REQUEST_NUMBER completed!${NC}"
        print_separator
    else
        echo -e "${RED}✗ Invalid request number: $REQUEST_NUMBER${NC}"
        echo -e "${RED}Valid request numbers are 1-8${NC}"
        echo ""
        echo "Run 'bash curl-examples.sh help' for usage information"
        exit 1
    fi
fi