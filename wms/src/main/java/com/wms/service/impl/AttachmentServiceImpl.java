package com.wms.service.impl;

import com.wms.dto.response.AttachmentResponse;
import com.wms.entity.Order;
import com.wms.entity.OrderAttachment;
import com.wms.entity.User;
import com.wms.enums.OrderStatus;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.OrderAttachmentRepository;
import com.wms.repository.OrderRepository;
import com.wms.repository.UserRepository;
import com.wms.service.AttachmentService;
import com.wms.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final Logger log = LogManager.getLogger(AttachmentServiceImpl.class);

    private final OrderAttachmentRepository attachmentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Override
    @Transactional
    public AttachmentResponse upload(Long orderId, MultipartFile file, String uploaderUsername) {
        log.info("User '{}' uploading attachment to order {}", uploaderUsername, orderId);

        Order order = findOrder(orderId);
        User uploader = findUser(uploaderUsername);

        if (uploader.getRole().name().equals("CLIENT")) {
            assertOwnership(order, uploaderUsername);
            if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.DECLINED) {
                throw new BusinessException(
                        "Attachments can only be added when the order is in CREATED or DECLINED status");
            }
        }

        String objectKey = storageService.store(file, orderId);

        OrderAttachment attachment = OrderAttachment.builder()
                .order(order)
                .uploadedBy(uploader)
                .originalName(file.getOriginalFilename())
                .storedName(objectKey)
                .filePath(objectKey)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .build();

        OrderAttachment saved = attachmentRepository.save(attachment);
        log.info("Attachment '{}' saved with id {} for order {}", file.getOriginalFilename(), saved.getId(), orderId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(Long orderId, String callerUsername, String callerRole) {
        Order order = findOrder(orderId);

        if ("CLIENT".equals(callerRole)) {
            assertOwnership(order, callerUsername);
        }

        return attachmentRepository.findByOrderId(orderId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderAttachment getAttachmentEntity(Long orderId, Long attachmentId,
                                                String callerUsername, String callerRole) {
        Order order = findOrder(orderId);

        if ("CLIENT".equals(callerRole)) {
            assertOwnership(order, callerUsername);
        }

        return attachmentRepository.findByIdAndOrderId(attachmentId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with id " + attachmentId + " for order " + orderId));
    }

    @Override
    @Transactional
    public void delete(Long orderId, Long attachmentId, String callerUsername) {
        log.info("User '{}' deleting attachment {} from order {}", callerUsername, attachmentId, orderId);

        Order order = findOrder(orderId);
        assertOwnership(order, callerUsername);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.DECLINED) {
            throw new BusinessException(
                    "Attachments can only be deleted when the order is in CREATED or DECLINED status");
        }

        OrderAttachment attachment = attachmentRepository.findByIdAndOrderId(attachmentId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with id " + attachmentId + " for order " + orderId));

        storageService.delete(attachment.getStoredName());
        attachmentRepository.delete(attachment);
        log.info("Attachment {} deleted from order {}", attachmentId, orderId);
    }


    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void assertOwnership(Order order, String username) {
        if (!order.getClient().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
    }

    private AttachmentResponse toResponse(OrderAttachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .orderId(a.getOrder().getId())
                .originalName(a.getOriginalName())
                .contentType(a.getContentType())
                .fileSize(a.getFileSize())
                .uploadedBy(a.getUploadedBy().getUsername())
                .uploadedAt(a.getUploadedAt())
                .downloadUrl("/api/orders/" + a.getOrder().getId() + "/attachments/" + a.getId())
                .build();
    }
}
