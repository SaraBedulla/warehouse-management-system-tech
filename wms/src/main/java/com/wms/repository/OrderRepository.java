package com.wms.repository;

import com.wms.entity.Order;
import com.wms.entity.User;
import com.wms.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClient(User client);

    List<Order> findByClientAndStatus(User client, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.orderNumber, 5) AS int)), 0) FROM Order o")
    Integer findMaxOrderSequence();
}
