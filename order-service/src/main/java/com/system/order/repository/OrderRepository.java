package com.system.order.repository;

import com.system.common.model.OrderStatus;
import com.system.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
