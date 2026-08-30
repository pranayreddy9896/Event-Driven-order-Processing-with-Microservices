package com.system.monolith.repository;

import com.system.monolith.model.MonolithPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonolithPaymentRepository extends JpaRepository<MonolithPayment, String> {
}
