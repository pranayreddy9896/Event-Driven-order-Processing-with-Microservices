package com.system.monolith.repository;

import com.system.monolith.model.MonolithOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonolithOrderRepository extends JpaRepository<MonolithOrder, String> {
}
