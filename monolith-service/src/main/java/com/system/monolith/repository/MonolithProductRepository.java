package com.system.monolith.repository;

import com.system.monolith.model.MonolithProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonolithProductRepository extends JpaRepository<MonolithProduct, String> {
}
