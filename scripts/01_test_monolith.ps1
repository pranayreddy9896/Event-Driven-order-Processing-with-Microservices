# ==============================================================================
# STEP 1: Monolith Benchmark & ACID Rollback Verification
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 1: Monolith Single ACID Transaction Benchmark" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api/monolith"

# 1. Check Initial Inventory
Write-Host "`n[1] Querying Monolith Inventory..." -ForegroundColor Yellow
$inventory = Invoke-RestMethod -Uri "$baseUrl/inventory" -Method Get
$inventory | Format-Table productId, productName, price, availableStock

# 2. Test Successful Monolith Order
Write-Host "`n[2] Placing Monolith Order (MacBook Pro x 1)..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = "CUST-ALICE"
    productId = "PROD-1"
    quantity = 1
    simulatePaymentFailure = $false
    simulatePaymentDelayMs = 0
} | ConvertTo-Json

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$response = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Post -Body $orderPayload -ContentType "application/json"
$stopwatch.Stop()

Write-Host "✓ Order Response Status: " -NoNewline; Write-Host $response.status -ForegroundColor Green
Write-Host "✓ Order ID: " -NoNewline; Write-Host $response.orderId -ForegroundColor Green
Write-Host "✓ Payment ID: " -NoNewline; Write-Host $response.paymentId -ForegroundColor Green
Write-Host "✓ Execution Latency: " -NoNewline; Write-Host "$($stopwatch.ElapsedMilliseconds)ms (Internal: $($response.executionDurationMs)ms)" -ForegroundColor Green

# 3. Test ACID Transaction Rollback on Failure
Write-Host "`n[3] Testing Monolith ACID Rollback (Simulating Payment Failure)..." -ForegroundColor Yellow
$failPayload = @{
    customerId = "CUST-BOB"
    productId = "PROD-1"
    quantity = 1
    simulatePaymentFailure = $true
    simulatePaymentDelayMs = 0
} | ConvertTo-Json

try {
    $failResponse = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Post -Body $failPayload -ContentType "application/json"
} catch {
    Write-Host "✓ Expected Transaction Rollback Caught!" -ForegroundColor Magenta
    Write-Host "  HTTP Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Magenta
}

# 4. Verify Stock was NOT deducted due to rollback
Write-Host "`n[4] Verifying Inventory State after Rollback..." -ForegroundColor Yellow
$updatedInventory = Invoke-RestMethod -Uri "$baseUrl/inventory" -Method Get
$updatedInventory | Format-Table productId, productName, price, availableStock
Write-Host "Observe: PROD-1 stock only decreased by 1 from the successful order. The failed order made zero DB changes." -ForegroundColor Cyan
