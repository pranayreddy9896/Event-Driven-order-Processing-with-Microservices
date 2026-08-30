package com.system.inventory.repository;

import com.system.inventory.model.InventoryReservation;
import com.system.inventory.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    List<InventoryReservation> findByOrderId(String orderId);

    Optional<InventoryReservation> findFirstByOrderIdAndStatus(String orderId, ReservationStatus status);
}
