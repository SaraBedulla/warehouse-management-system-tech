package com.wms.repository;

import com.wms.entity.OrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Long> {

    List<OrderAttachment> findByOrderId(Long orderId);

    Optional<OrderAttachment> findByIdAndOrderId(Long id, Long orderId);

    void deleteByOrderId(Long orderId);
}
