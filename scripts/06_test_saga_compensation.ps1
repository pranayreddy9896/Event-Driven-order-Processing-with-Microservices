# ==============================================================================
# STEP 6: Saga Pattern & Compensating Transactions
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 6: Saga Pattern - Failure & Compensating Rollback" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$orderUrl = "http://localhost:8081/api/orders"
$paymentConfigUrl = "http://localhost:8082/api/payment/config"
$inventoryUrl = "http://localhost:8083/api/inventory"

# 1. Check stock before starting
Write-Host "`n[1] Checking initial stock for PROD-2 (Keychron Keyboard)..." -ForegroundColor Yellow
$prodBefore = Invoke-RestMethod -Uri "$inventoryUrl/PROD-2" -Method Get
Write-Host "  Available Stock: $($prodBefore.availableQuantity) | Reserved: $($prodBefore.reservedQuantity)" -ForegroundColor Cyan

# 2. Force Payment Failure to trigger Saga Compensation
Write-Host "`n[2] Configuring Payment Service to FORCE FAILURE (simulated card decline)..." -ForegroundColor Yellow
$configPayload = @{ forceFailure = $true; artificialDelayMs = 500 } | ConvertTo-Json
Invoke-RestMethod -Uri $paymentConfigUrl -Method Post -Body $configPayload -ContentType "application/json" | Out-Null
Write-Host "✓ Payment Service set to fail next transaction" -ForegroundColor Magenta

# 3. Create Order
Write-Host "`n[3] Submitting Order (Order -> Inventory Reserves Stock -> Payment Fails -> Compensation Releases Stock)..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = "CUST-SAGA-FAIL"
    productId = "PROD-2"
    quantity = 1
    unitPrice = 99.99
} | ConvertTo-Json

$order = Invoke-RestMethod -Uri $orderUrl -Method Post -Body $orderPayload -ContentType "application/json"
$orderId = $order.orderId
Write-Host "✓ Placed Order: $orderId" -ForegroundColor Green

# 4. Monitor Saga State transitions
Write-Host "`n[4] Observing Saga Choreography & Compensating Rollback..." -ForegroundColor Yellow
$attempts = 0
while ($attempts -lt 8) {
    Start-Sleep -Milliseconds 600
    $attempts++
    $current = Invoke-RestMethod -Uri "$orderUrl/$orderId" -Method Get
    Write-Host "  Attempt $attempts: Order Status = $($current.status)" -ForegroundColor ($current.status -eq "CANCELLED" ? "Red" : "Yellow")
    if ($current.status -eq "CANCELLED") {
        Write-Host "  Reason: $($current.failureReason)" -ForegroundColor Magenta
        break
    }
}

# 5. Verify Inventory Compensating Transaction Restored Stock
Write-Host "`n[5] Verifying Inventory Compensating Transaction (Stock Rollback)..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
$prodAfter = Invoke-RestMethod -Uri "$inventoryUrl/PROD-2" -Method Get
Write-Host "  Available Stock After Compensation: $($prodAfter.availableQuantity) | Reserved: $($prodAfter.reservedQuantity)" -ForegroundColor Green

if ($prodAfter.availableQuantity -eq $prodBefore.availableQuantity) {
    Write-Host "✓ SUCCESS: Stock was fully restored by the compensating transaction after payment failure!" -ForegroundColor Green
} else {
    Write-Host "! Stock mismatch: Before=$($prodBefore.availableQuantity), After=$($prodAfter.availableQuantity)" -ForegroundColor Red
}

# Reset Payment Config
Invoke-RestMethod -Uri "$paymentConfigUrl/reset" -Method Post | Out-Null
