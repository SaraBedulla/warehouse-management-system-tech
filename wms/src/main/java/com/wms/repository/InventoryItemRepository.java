package com.wms.repository;

import com.wms.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    boolean existsByItemNameIgnoreCase(String itemName);

    List<InventoryItem> findByItemNameContainingIgnoreCase(String name);
}
