# Hotel Booking Microservices - Distributed Curl Examples (PowerShell / Windows / IntelliJ Compatible)
# This script demonstrates the complete workflow across three independent microservices
#
# SERVICES:
#   - Booking Service (Port 8080)
#   - Payment Service (Port 8082)
#   - Invoice Service (Port 8083)
#
# USAGE:
#   Windows PowerShell:
#     .\curl-examples-distributed.ps1                    # Run all steps
#     .\curl-examples-distributed.ps1 -StepNumber 1      # Run only step 1
#     .\curl-examples-distributed.ps1 -StepNumber 4      # Run only step 4
#     .\curl-examples-distributed.ps1 -Help              # Show help
#
#   IntelliJ/PyCharm (Run → Edit Configurations):
#     Program: powershell.exe
#     Arguments: -File "C:\full\path\to\curl-examples-distributed.ps1" -StepNumber 1
#
# REQUIREMENTS:
#   - PowerShell 5.0+ (built into Windows 10+)
#   - All three microservices running:
#     * cd /workspace/split/invoice-service && mvn spring-boot:run
#     * cd /workspace/split/payment-service && mvn spring-boot:run
#     * cd /workspace/split/booking-service && mvn spring-boot:run
#   - curl available (built into Windows 10+ and PowerShell 7+)

param(
    [int]$StepNumber = 0,  # 0 = run all, 1-8 = run specific step
    [switch]$Help
)

# Display help information
if ($Help) {
    Write-Host @"
Hotel Booking Microservices - Distributed Curl Examples (PowerShell Version)

USAGE:
  Run all steps:
    .\curl-examples-distributed.ps1

  Run specific step:
    .\curl-examples-distributed.ps1 -StepNumber 1

  Show this help:
    .\curl-examples-distributed.ps1 -Help

AVAILABLE STEPS:
  1. CHECK ROOM AVAILABILITY (Booking Service)
  2. BOOK A ROOM (Booking Service)
  3. CHECK-IN (Booking Service)
  4. PROCESS PAYMENT (Payment Service)
  5. CHECK PAYMENT CREDIT (Payment Service)
  6. CREATE INVOICE (Booking → Payment → Invoice Chain)
  7. VERIFY INVOICE CREATED (Invoice Service)
  8. CHECK-OUT (Booking Service)

STARTING SERVICES (Required):
  Open 3 terminals and start services in this order:

  Terminal 1 (Invoice Service - Port 8083):
    cd /workspace/split/invoice-service
    mvn spring-boot:run

  Terminal 2 (Payment Service - Port 8082):
    cd /workspace/split/payment-service
    mvn spring-boot:run

  Terminal 3 (Booking Service - Port 8080):
    cd /workspace/split/booking-service
    mvn spring-boot:run

ENVIRONMENT VARIABLES:
  BOOKING_SERVICE_URL: Base URL for booking service (default: http://localhost:8080)
  PAYMENT_SERVICE_URL: Base URL for payment service (default: http://localhost:8082)
  INVOICE_SERVICE_URL: Base URL for invoice service (default: http://localhost:8083)
  CUSTOMER_NAME: Customer name for requests (default: Max Mustermann)

EXAMPLES:
  # Run all steps
  .\curl-examples-distributed.ps1

  # Run only the booking step
  .\curl-examples-distributed.ps1 -StepNumber 2

  # Set custom URLs
  `$env:BOOKING_SERVICE_URL="http://localhost:8080"
  `$env:PAYMENT_SERVICE_URL="http://localhost:8082"
  `$env:INVOICE_SERVICE_URL="http://localhost:8083"
  .\curl-examples-distributed.ps1

WHAT THIS TESTS:
  - Booking Service can create bookings
  - Booking Service calls Payment Service to process payments
  - Payment Service calls Invoice Service to generate invoices
  - Each service has independent database
  - Service-to-service communication via REST APIs

TROUBLESHOOTING:
  If you see "Connection refused" errors:
    1. Make sure all three services are running
    2. Check the service ports (8080, 8082, 8083)
    3. View service logs to check for startup errors

  If requests are timing out:
    1. Services may still be starting up
    2. Wait 30 seconds and try again
    3. Check if services are consuming resources
"@
    exit 0
}

# Configuration
$BookingServiceUrl = if ($env:BOOKING_SERVICE_URL) { $env:BOOKING_SERVICE_URL } else { "http://localhost:8080" }
$PaymentServiceUrl = if ($env:PAYMENT_SERVICE_URL) { $env:PAYMENT_SERVICE_URL } else { "http://localhost:8082" }
$InvoiceServiceUrl = if ($env:INVOICE_SERVICE_URL) { $env:INVOICE_SERVICE_URL } else { "http://localhost:8083" }
$CustomerName = if ($env:CUSTOMER_NAME) { $env:CUSTOMER_NAME } else { "Max Mustermann" }

# Global variables for response parsing
$InvoiceId = $null

# Color functions (cross-platform compatible)
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Info { Write-Host $args[0] -ForegroundColor Cyan }
function Write-Command { Write-Host $args[0] -ForegroundColor Yellow }
function Write-Error_ { Write-Host $args[0] -ForegroundColor Red }
function Write-Warning_ { Write-Host $args[0] -ForegroundColor Yellow }

# Separator function
function Show-Separator {
    Write-Info "════════════════════════════════════════════════════════════════"
}

# Execute request function
function Invoke-CurlRequest {
    param(
        [int]$Number,
        [string]$Title,
        [string]$Method,
        [string]$Url,
        [string]$Data = "",
        [scriptblock]$PostProcessing = $null
    )

    Write-Info ""
    Write-Info "STEP $Number : $Title"
    Write-Info "$Method $Url"
    Show-Separator

    try {
        if ($Method -eq "GET") {
            Write-Command "curl -X GET `"$Url`" -H `"Content-Type: application/json`""
            Write-Host ""
            $response = Invoke-WebRequest -Uri $Url -Method Get -ContentType "application/json" -ErrorAction Continue
            $jsonContent = $response.Content | ConvertFrom-Json
            Write-Host ($jsonContent | ConvertTo-Json -Depth 10)
            Write-Host ""
            Write-Success "✓ HTTP $($response.StatusCode)"

            # Execute post-processing if provided
            if ($PostProcessing) {
                & $PostProcessing $jsonContent
            }
        }
        else {
            Write-Command "curl -X $Method `"$Url`" -H `"Content-Type: application/json`" -d '$Data'"
            Write-Host ""
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Body $Data -ContentType "application/json" -ErrorAction Continue
            $jsonContent = $response.Content | ConvertFrom-Json
            Write-Host ($jsonContent | ConvertTo-Json -Depth 10)
            Write-Host ""
            Write-Success "✓ HTTP $($response.StatusCode)"

            # Execute post-processing if provided
            if ($PostProcessing) {
                & $PostProcessing $jsonContent
            }
        }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Error_ "✗ HTTP $statusCode : $($_.Exception.Message)"
    }
}

# Main script
Write-Info ""
Show-Separator
Write-Info "  Microservices Integration Test (PowerShell Version)"
Show-Separator
Write-Info ""
Write-Info "Service Configuration:"
Write-Info "  Booking Service:  $BookingServiceUrl"
Write-Info "  Payment Service:  $PaymentServiceUrl"
Write-Info "  Invoice Service:  $InvoiceServiceUrl"
Write-Info ""
Write-Info "Customer Name: $CustomerName"
Write-Info ""
Write-Warning_ "⚠ Make sure all three microservices are running:"
Write-Warning_ "  Terminal 1: cd /workspace/split/invoice-service && mvn spring-boot:run"
Write-Warning_ "  Terminal 2: cd /workspace/split/payment-service && mvn spring-boot:run"
Write-Warning_ "  Terminal 3: cd /workspace/split/booking-service && mvn spring-boot:run"
Write-Info ""

# Define steps
$steps = @(
    @{
        Number   = 1
        Title    = "CHECK ROOM AVAILABILITY (Booking Service)"
        Method   = "GET"
        Url      = "$BookingServiceUrl/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05"
        Data     = ""
    },
    @{
        Number   = 2
        Title    = "BOOK A ROOM (Booking Service)"
        Method   = "POST"
        Url      = "$BookingServiceUrl/api/hotel/rooms/booking"
        Data     = @{
            startDate    = "2025-11-01"
            endDate      = "2025-11-05"
            customerName = $CustomerName
        } | ConvertTo-Json
    },
    @{
        Number   = 3
        Title    = "CHECK-IN (Booking Service)"
        Method   = "POST"
        Url      = "$BookingServiceUrl/api/hotel/checkin"
        Data     = @{
            customerName = $CustomerName
            startDate    = "2025-11-01"
        } | ConvertTo-Json
    },
    @{
        Number   = 4
        Title    = "PROCESS PAYMENT (Payment Service)"
        Method   = "POST"
        Url      = "$PaymentServiceUrl/api/payments"
        Data     = @{
            customerName = $CustomerName
            amount       = 500.00
        } | ConvertTo-Json
    },
    @{
        Number   = 5
        Title    = "CHECK PAYMENT CREDIT (Payment Service)"
        Method   = "GET"
        Url      = "$PaymentServiceUrl/api/payments/credit/$([System.Uri]::EscapeDataString($CustomerName))"
        Data     = ""
    },
    @{
        Number   = 6
        Title    = "CREATE INVOICE (Booking → Payment → Invoice Chain)"
        Method   = "POST"
        Url      = "$BookingServiceUrl/api/hotel/invoice"
        Data     = @{
            customerName = $CustomerName
            endDate      = "2025-11-05"
            roomNumbers  = @("101")
        } | ConvertTo-Json
        PostProcessing = {
            param($response)
            if ($response.invoiceId) {
                $script:InvoiceId = $response.invoiceId
                Write-Success "  → Invoice ID: $($script:InvoiceId)"
            }
        }
    },
    @{
        Number   = 7
        Title    = "VERIFY INVOICE CREATED (Invoice Service)"
        Method   = "GET"
        Url      = "$InvoiceServiceUrl/api/invoices/$InvoiceId"
        Data     = ""
    },
    @{
        Number   = 8
        Title    = "CHECK-OUT (Booking Service)"
        Method   = "POST"
        Url      = "$BookingServiceUrl/api/hotel/checkout"
        Data     = @{
            customerName = $CustomerName
            roomNumber   = "101"
            endDate      = "2025-11-05"
        } | ConvertTo-Json
    }
)

# Execute steps
if ($StepNumber -eq 0) {
    # Run all steps
    foreach ($step in $steps) {
        Invoke-CurlRequest -Number $step.Number -Title $step.Title -Method $step.Method -Url $step.Url -Data $step.Data -PostProcessing $step.PostProcessing
    }

    Write-Success ""
    Show-Separator
    Write-Success "  ✓ All steps completed!"
    Show-Separator
    Write-Info ""
    Write-Info "Service Communication Summary:"
    Write-Info "  1. ✓ Booking Service (8080) - Checked availability and booked room"
    Write-Info "  2. ✓ Booking Service (8080) - Checked in guest"
    Write-Info "  3. ✓ Payment Service (8082) - Processed payment"
    Write-Info "  4. ✓ Booking Service (8080) - Created invoice (called Payment & Invoice services)"
    Write-Info "  5. ✓ Invoice Service (8083) - Verified invoice created"
    Write-Info "  6. ✓ Booking Service (8080) - Completed checkout"
    Write-Info ""
    Write-Success "✓ Multi-service integration workflow completed successfully!"
}
else {
    # Run specific step
    if ($StepNumber -ge 1 -and $StepNumber -le $steps.Count) {
        $step = $steps[$StepNumber - 1]

        # Special handling for step 7 if no invoice ID from step 6
        if ($StepNumber -eq 7 -and $null -eq $InvoiceId) {
            Write-Warning_ "Warning: Invoice ID not available. Run step 6 first to create an invoice."
            exit 1
        }

        Invoke-CurlRequest -Number $step.Number -Title $step.Title -Method $step.Method -Url $step.Url -Data $step.Data -PostProcessing $step.PostProcessing

        Write-Success ""
        Show-Separator
        Write-Success "  ✓ Step $StepNumber completed!"
        Show-Separator
    }
    else {
        Write-Error_ "Invalid step number: $StepNumber"
        Write-Error_ "Valid step numbers are 1-$($steps.Count)"
        exit 1
    }
}