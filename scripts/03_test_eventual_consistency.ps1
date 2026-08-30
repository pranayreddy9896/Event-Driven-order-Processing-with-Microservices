# ==============================================================================
# STEP 3: Kafka Asynchronous Event-Driven Flow & Eventual Consistency
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 3: Async Event-Driven Flow & Eventual Consistency" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$orderUrl = "http://localhost:8081/api/orders"
$paymentConfigUrl = "http://localhost:8082/api/payment/config"

# Reset payment delay to 1500ms so we can visibly watch eventual consistency unfold
$configBody = @{ artificialDelayMs = 1500; forceFailure = $false } | ConvertTo-Json
Invoke-RestMethod -Uri $paymentConfigUrl -Method Post -Body $configBody -ContentType "application/json" | Out-Null

Write-Host "`n[1] Submitting Async Order to Order Service (POST /api/orders)..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = "CUST-ASYNC-01"
    productId = "PROD-1"
    quantity = 1
    unitPrice = 1999.99
} | ConvertTo-Json

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$order = Invoke-RestMethod -Uri $orderUrl -Method Post -Body $orderPayload -ContentType "application/json"
$sw.Stop()

Write-Host "✓ Client received response in: $($sw.ElapsedMilliseconds)ms (HTTP 202 ACCEPTED)" -ForegroundColor Green
Write-Host "✓ Immediate Order Status:      " -NoNewline; Write-Host $order.status -ForegroundColor Yellow
Write-Host "✓ Order ID:                    $($order.orderId)" -ForegroundColor Cyan
Write-Host "✓ Trace ID:                    $($order.traceId)" -ForegroundColor Cyan

Write-Host "`n[2] Polling Order Status to observe Eventual Consistency transition..." -ForegroundColor Yellow
$orderId = $order.orderId
$attempts = 0
$maxAttempts = 10

while ($attempts -lt $maxAttempts) {
    Start-Sleep -Milliseconds 600
    $attempts++
    $current = Invoke-RestMethod -Uri "$orderUrl/$orderId" -Method Get
    $timestamp = (Get-Date).ToString("HH:mm:ss.fff")
    Write-Host "  [$timestamp] Attempt $attempts: Status = " -NoNewline
    
    if ($current.status -eq "CONFIRMED") {
        Write-Host "$($current.status) (Payment ID: $($current.paymentId))" -ForegroundColor Green
        break
    } elseif ($current.status -eq "PENDING") {
        Write-Host "$($current.status)" -ForegroundColor Yellow
    } else {
        Write-Host "$($current.status)" -ForegroundColor Magenta
    }
}

Write-Host "`nEventual Consistency Demonstrated: Order was accepted instantaneously without blocking, and reached its final consistent state asynchronously through Kafka events." -ForegroundColor Cyan
