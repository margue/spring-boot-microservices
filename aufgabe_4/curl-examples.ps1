# Hotel Booking Service - Comprehensive Curl Examples (PowerShell / Windows / IntelliJ Compatible)
# This script demonstrates all available API endpoints with executable curl commands
#
# USAGE:
#   Windows PowerShell:
#     .\curl-examples.ps1                    # Run all requests
#     .\curl-examples.ps1 -RequestNumber 1   # Run only request 1
#     .\curl-examples.ps1 -RequestNumber 2   # Run only request 2
#     .\curl-examples.ps1 -Help              # Show help
#
#   IntelliJ/PyCharm (Run → Edit Configurations):
#     Program: powershell.exe
#     Arguments: -File "C:\full\path\to\curl-examples.ps1" -RequestNumber 1
#
# REQUIREMENTS:
#   - PowerShell 5.0+ (built into Windows 10+)
#   - curl available (built into Windows 10+ and PowerShell 7+)
#   - Or install curl separately: https://curl.se/download.html

param(
    [int]$RequestNumber = 0,  # 0 = run all, 1-8 = run specific request
    [switch]$Help
)

# Display help information
if ($Help) {
    Write-Host @"
Hotel Booking Service - Curl Examples (PowerShell Version)

USAGE:
  Run all requests:
    .\curl-examples.ps1

  Run specific request:
    .\curl-examples.ps1 -RequestNumber 1

  Show this help:
    .\curl-examples.ps1 -Help

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
  .\curl-examples.ps1

  # Run only the booking request
  .\curl-examples.ps1 -RequestNumber 2

  # Set custom base URL for different environment
  `$env:BASE_URL="http://booking-service.example.com"
  .\curl-examples.ps1 -RequestNumber 1

TROUBLESHOOTING:
  If you see "curl: command not found", install curl:
    - Windows 10+: curl should be built-in
    - Windows PowerShell 7+: use 'pwsh' instead of 'powershell'
    - Or download from: https://curl.se/download.html

  If you see "execution policy" error:
    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
"@
    exit 0
}

# Configuration
$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
$CustomerName = if ($env:CUSTOMER_NAME) { $env:CUSTOMER_NAME } else { "Max Mustermann" }

# Color functions (cross-platform compatible)
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Info { Write-Host $args[0] -ForegroundColor Cyan }
function Write-Command { Write-Host $args[0] -ForegroundColor Yellow }
function Write-Error_ { Write-Host $args[0] -ForegroundColor Red }

# Separator function
function Show-Separator {
    Write-Info "================================================================"
}

# Execute request function
function Invoke-CurlRequest {
    param(
        [int]$Number,
        [string]$Title,
        [string]$Method,
        [string]$Endpoint,
        [string]$Data = ""
    )

    Write-Info ""
    Write-Info "REQUEST $Number : $Title"
    Write-Info "$Method $Endpoint"
    Show-Separator

    try {
        if ($Method -eq "GET") {
            $Uri = "$BaseUrl$Endpoint"
            Write-Command "curl -X GET `"$Uri`" -H `"Content-Type: application/json`""
            Write-Host ""
            $response = Invoke-WebRequest -Uri $Uri -Method Get -ContentType "application/json" -ErrorAction Continue
            Write-Host $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
        }
        else {
            $Uri = "$BaseUrl$Endpoint"
            Write-Command "curl -X $Method `"$Uri`" -H `"Content-Type: application/json`" -d '$Data'"
            Write-Host ""
            $response = Invoke-WebRequest -Uri $Uri -Method $Method -Body $Data -ContentType "application/json" -ErrorAction Continue
            Write-Host $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
        }
        Write-Host ""
        Write-Success "✓ Request completed successfully (HTTP $($response.StatusCode))"
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Error_ "✗ Request failed (HTTP $statusCode): $($_.Exception.Message)"
    }
}

# Main script
Write-Info ""
Write-Info "════════════════════════════════════════════════════════════════"
Write-Info "  Hotel Booking Service - API Examples (PowerShell Version)"
Write-Info "════════════════════════════════════════════════════════════════"
Write-Info ""
Write-Info "Base URL: $BaseUrl"
Write-Info "Customer Name: $CustomerName"
Write-Info ""

# Define requests
$requests = @(
    @{
        Number   = 1
        Title    = "CHECK ROOM AVAILABILITY"
        Method   = "GET"
        Endpoint = "/api/hotel/rooms/availability?startDate=2025-11-01&endDate=2025-11-05"
        Data     = ""
    },
    @{
        Number   = 2
        Title    = "BOOK A ROOM"
        Method   = "POST"
        Endpoint = "/api/hotel/rooms/booking"
        Data     = @{
            startDate    = "2025-11-01"
            endDate      = "2025-11-05"
            customerName = $CustomerName
        } | ConvertTo-Json
    },
    @{
        Number   = 3
        Title    = "CHECK-IN"
        Method   = "POST"
        Endpoint = "/api/hotel/checkin"
        Data     = @{
            customerName = $CustomerName
            startDate    = "2025-11-01"
        } | ConvertTo-Json
    },
    @{
        Number   = 4
        Title    = "PROCESS PAYMENT"
        Method   = "POST"
        Endpoint = "/api/payments"
        Data     = @{
            customerName = $CustomerName
            amount       = 500.00
        } | ConvertTo-Json
    },
    @{
        Number   = 5
        Title    = "GET REMAINING CREDIT"
        Method   = "GET"
        Endpoint = "/api/payments/credit/$([System.Uri]::EscapeDataString($CustomerName))"
        Data     = ""
    },
    @{
        Number   = 6
        Title    = "CREATE INVOICE"
        Method   = "POST"
        Endpoint = "/api/hotel/invoice"
        Data     = @{
            customerName = $CustomerName
            endDate      = "2025-11-05"
            roomNumbers  = @("101")
        } | ConvertTo-Json
    },
    @{
        Number   = 7
        Title    = "GET REMAINING CREDIT AFTER PAYMENT"
        Method   = "GET"
        Endpoint = "/api/payments/credit/$([System.Uri]::EscapeDataString($CustomerName))"
        Data     = ""
    },
    @{
        Number   = 8
        Title    = "CHECK-OUT"
        Method   = "POST"
        Endpoint = "/api/hotel/checkout"
        Data     = @{
            customerName = $CustomerName
            roomNumber   = "101"
            endDate      = "2025-11-05"
        } | ConvertTo-Json
    }
)

# Execute requests
if ($RequestNumber -eq 0) {
    # Run all requests
    foreach ($request in $requests) {
        Invoke-CurlRequest -Number $request.Number -Title $request.Title -Method $request.Method -Endpoint $request.Endpoint -Data $request.Data
    }
    Write-Success ""
    Write-Success "════════════════════════════════════════════════════════════════"
    Write-Success "  ✓ All requests completed!"
    Write-Success "════════════════════════════════════════════════════════════════"
}
else {
    # Run specific request
    $request = $requests[$RequestNumber - 1]
    if ($request) {
        Invoke-CurlRequest -Number $request.Number -Title $request.Title -Method $request.Method -Endpoint $request.Endpoint -Data $request.Data
        Write-Success ""
        Write-Success "════════════════════════════════════════════════════════════════"
        Write-Success "  ✓ Request $RequestNumber completed!"
        Write-Success "════════════════════════════════════════════════════════════════"
    }
    else {
        Write-Error_ "Invalid request number: $RequestNumber"
        Write-Error_ "Valid request numbers are 1-8"
        exit 1
    }
}