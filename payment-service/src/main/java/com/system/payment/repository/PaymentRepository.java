package com.system.payment.repository;

import com.system.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);
    Optional<Payment> findFirstByOrderIdAndStatus(String orderId, String status);
}
