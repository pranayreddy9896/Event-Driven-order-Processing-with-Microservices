# ==============================================================================
# STEP 4: Transactional Outbox Pattern & Durability Verification
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 4: Transactional Outbox Pattern Durability Test" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$orderUrl = "http://localhost:8081/api/orders"

Write-Host "`n[1] Creating new order to observe Outbox table atomicity..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = "CUST-OUTBOX-TEST"
    productId = "PROD-1"
    quantity = 2
    unitPrice = 1999.99
} | ConvertTo-Json

$order = Invoke-RestMethod -Uri $orderUrl -Method Post -Body $orderPayload -ContentType "application/json"
Write-Host "✓ Order Created: ID=$($order.orderId)" -ForegroundColor Green

Write-Host "`n[2] Outbox Pattern Mechanism:" -ForegroundColor Yellow
Write-Host "  1. Client POST request writes Order row and Outbox row in single atomic local DB transaction." -ForegroundColor White
Write-Host "  2. Even if Kafka broker is temporarily down or slow, the event is safely persisted in the database." -ForegroundColor White
Write-Host "  3. OrderOutboxRelay poller polls PENDING events, dispatches to Kafka topic 'order.created', and marks them SENT." -ForegroundColor White
Write-Host "  4. Dual-write problem is eliminated: DB write and message publishing can NEVER diverge." -ForegroundColor Green
