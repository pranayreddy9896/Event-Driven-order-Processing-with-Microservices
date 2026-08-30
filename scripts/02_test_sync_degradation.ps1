# ==============================================================================
# STEP 2: Synchronous Microservices & Latency Degradation Test
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 2: Synchronous Microservice Latency Degradation" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$orderUrl = "http://localhost:8081/api/orders/sync"
$paymentUrl = "http://localhost:8082/api/payment/config"

# 1. Baseline Fast Synchronous Call
Write-Host "`n[1] Baseline Fast Synchronous Order (0ms downstream delay)..." -ForegroundColor Yellow
$fastPayload = @{
    customerId = "CUST-FAST"
    productId = "PROD-1"
    quantity = 1
    unitPrice = 1999.99
} | ConvertTo-Json

$resFast = Invoke-RestMethod -Uri "$orderUrl?delayMs=0" -Method Post -Body $fastPayload -ContentType "application/json"
Write-Host "✓ Total Order Latency: " -NoNewline; Write-Host "$($resFast.totalDurationMs)ms" -ForegroundColor Green
Write-Host "✓ Payment Duration: " -NoNewline; Write-Host "$($resFast.paymentServiceDurationMs)ms" -ForegroundColor Green

# 2. Inject 3000ms Latency into Payment Service
Write-Host "`n[2] Deliberately injecting 3000ms latency into Payment Service..." -ForegroundColor Yellow
$slowPayload = @{
    customerId = "CUST-SLOW"
    productId = "PROD-1"
    quantity = 1
    unitPrice = 1999.99
} | ConvertTo-Json

Write-Host "Sending synchronous order request to Order Service (:8081)..." -ForegroundColor Cyan
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$resSlow = Invoke-RestMethod -Uri "$orderUrl?delayMs=3000" -Method Post -Body $slowPayload -ContentType "application/json"
$sw.Stop()

Write-Host "========================================================" -ForegroundColor Red
Write-Host " CASCADING LATENCY & THREAD STARVATION OBSERVED:" -ForegroundColor Red
Write-Host "========================================================" -ForegroundColor Red
Write-Host "  Order Service Total Response Time: $($sw.ElapsedMilliseconds)ms" -ForegroundColor Red
Write-Host "  Payment Service Latency:           $($resSlow.paymentServiceDurationMs)ms" -ForegroundColor Red
Write-Host "  Order Service Status:              $($resSlow.status)" -ForegroundColor Yellow
Write-Host "`nInsight: When services communicate synchronously over HTTP, slow downstream dependencies directly block upstream server worker threads, multiplying failure risk across the system." -ForegroundColor White
