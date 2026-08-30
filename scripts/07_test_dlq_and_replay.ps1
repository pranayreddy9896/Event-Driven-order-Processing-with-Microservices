# ==============================================================================
# STEP 7: Dead Letter Queue (DLQ) & Admin Replay Tool Test
# ==============================================================================
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " STEP 7: Dead Letter Queue (DLQ) & Message Replay" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$dlqUrl = "http://localhost:8085/api/dlq"
$paymentConfigUrl = "http://localhost:8082/api/payment/config"
$orderUrl = "http://localhost:8081/api/orders"

# 1. Arm Poison Pill in Payment Service
Write-Host "`n[1] Arming Payment Service with Poison Pill (Unrecoverable Exception)..." -ForegroundColor Yellow
$configPayload = @{ forcePoisonPill = $true } | ConvertTo-Json
Invoke-RestMethod -Uri $paymentConfigUrl -Method Post -Body $configPayload -ContentType "application/json" | Out-Null
Write-Host "✓ Poison pill armed." -ForegroundColor Magenta

# 2. Trigger Order that will hit Poison Pill
Write-Host "`n[2] Triggering Order to produce Dead-Lettered Message..." -ForegroundColor Yellow
$orderPayload = @{
    customerId = "CUST-POISON-PILL"
    productId = "PROD-1"
    quantity = 1
    unitPrice = 1999.99
} | ConvertTo-Json

$order = Invoke-RestMethod -Uri $orderUrl -Method Post -Body $orderPayload -ContentType "application/json"
Write-Host "✓ Order sent: $($order.orderId)" -ForegroundColor Cyan

Write-Host "`n[3] Waiting for Consumer retries to exhaust and route message to DLT..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 3. Query DLQ Admin API for captured messages
Write-Host "`n[4] Querying DLQ Admin Service (:8085/api/dlq/messages)..." -ForegroundColor Yellow
$messages = Invoke-RestMethod -Uri "$dlqUrl/messages?status=POISONED" -Method Get

if ($messages.Count -gt 0) {
    Write-Host "✓ Found $($messages.Count) Poisoned Message(s) in Dead Letter Topic!" -ForegroundColor Green
    $msg = $messages[0]
    Write-Host "  DLQ ID:         $($msg.id)" -ForegroundColor White
    Write-Host "  Original Topic: $($msg.originalTopic)" -ForegroundColor White
    Write-Host "  DLT Topic:      $($msg.dltTopic)" -ForegroundColor White
    Write-Host "  Exception:      $($msg.exceptionMessage)" -ForegroundColor Red
    
    # 4. Disarm Poison Pill & Replay Message
    Write-Host "`n[5] Disarming Poison Pill (Fixing Root Cause) and Replaying Message..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "$paymentConfigUrl/reset" -Method Post | Out-Null
    
    $replayed = Invoke-RestMethod -Uri "$dlqUrl/replay/$($msg.id)" -Method Post
    Write-Host "✓ Message replayed! Status = $($replayed.status) at $($replayed.replayedAt)" -ForegroundColor Green
    Write-Host "  Message was re-published to topic '$($replayed.originalTopic)' for clean processing!" -ForegroundColor Cyan
} else {
    Write-Host "! No DLQ messages found yet (ensure services are active)." -ForegroundColor Yellow
}
