package com.wms.service.impl;

import com.wms.dto.request.CreateOrderRequest;
import com.wms.dto.request.DeclineOrderRequest;
import com.wms.dto.request.OrderItemRequest;
import com.wms.dto.response.AvailableAction;
import com.wms.dto.response.OrderItemResponse;
import com.wms.dto.response.OrderResponse;
import com.wms.entity.InventoryItem;
import com.wms.entity.Order;
import com.wms.entity.OrderItem;
import com.wms.entity.User;
import com.wms.enums.OrderStatus;
import com.wms.enums.Role;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.InventoryItemRepository;
import com.wms.repository.OrderRepository;
import com.wms.repository.UserRepository;
import com.wms.service.OrderActionResolver;
import com.wms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LogManager.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderActionResolver actionResolver;

    // ─── CLIENT ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String username) {
        log.info("Creating order for client '{}'", username);
        User client = findUserByUsername(username);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .client(client)
                .status(OrderStatus.CREATED)
                .deadlineDate(request.getDeadlineDate())
                .build();

        if (request.getItems() != null) {
            buildOrderItems(order, request.getItems());
        }

        Order saved = orderRepository.save(order);
        log.info("Order '{}' created for client '{}'", saved.getOrderNumber(), username);
        return toResponse(saved, client.getRole());
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, CreateOrderRequest request, String username) {
        log.info("Updating order id {} for client '{}'", id, username);
        Order order = findOrderById(id);
        assertOwnership(order, username);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.DECLINED) {
            throw new BusinessException("Order can only be updated when in CREATED or DECLINED status");
        }

        order.getItems().clear();
        if (request.getItems() != null) {
            buildOrderItems(order, request.getItems());
        }
        if (request.getDeadlineDate() != null) {
            order.setDeadlineDate(request.getDeadlineDate());
        }
        if (order.getStatus() == OrderStatus.DECLINED) {
            order.setDeclineReason(null);
            order.setStatus(OrderStatus.CREATED);
        }

        Order saved = orderRepository.save(order);
        log.info("Order '{}' updated", saved.getOrderNumber());
        return toResponse(saved, order.getClient().getRole());
    }

    @Override
    @Transactional
    public OrderResponse submitOrder(Long id, String username) {
        log.info("Submitting order id {} by client '{}'", id, username);
        Order order = findOrderById(id);
        assertOwnership(order, username);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Only orders in CREATED status can be submitted");
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessException("Cannot submit an order with no items");
        }

        order.setStatus(OrderStatus.AWAITING_APPROVAL);
        Order saved = orderRepository.save(order);
        log.info("Order '{}' submitted for approval", saved.getOrderNumber());
        return toResponse(saved, order.getClient().getRole());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String username) {
        log.info("Cancelling order id {} by client '{}'", id, username);
        Order order = findOrderById(id);
        assertOwnership(order, username);

        if (order.getStatus() == OrderStatus.FULFILLED || order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessException("Order cannot be cancelled — it is already " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELED);
        Order saved = orderRepository.save(order);
        log.info("Order '{}' cancelled", saved.getOrderNumber());
        return toResponse(saved, order.getClient().getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String username, OrderStatus status) {
        User client = findUserByUsername(username);
        List<Order> orders = (status != null)
                ? orderRepository.findByClientAndStatus(client, status)
                : orderRepository.findByClient(client);
        return orders.stream().map(o -> toResponse(o, Role.CLIENT)).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponse getMyOrderById(Long id, String username) {
        Order order = findOrderById(id);
        assertOwnership(order, username);
        return toResponse(order, Role.CLIENT);
    }

    // ─── WAREHOUSE MANAGER ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders = (status != null)
                ? orderRepository.findByStatus(status)
                : orderRepository.findAll();
        return orders.stream().map(o -> toResponse(o, Role.WAREHOUSE_MANAGER)).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponse getOrderById(Long id) {
        return toResponse(findOrderById(id), Role.WAREHOUSE_MANAGER);
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(Long id) {
        log.info("Approving order id {}", id);
        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.AWAITING_APPROVAL) {
            throw new BusinessException("Only orders in AWAITING_APPROVAL status can be approved");
        }

        order.setStatus(OrderStatus.APPROVED);
        Order saved = orderRepository.save(order);
        log.info("Order '{}' approved", saved.getOrderNumber());
        return toResponse(saved, Role.WAREHOUSE_MANAGER);
    }

    @Override
    @Transactional
    public OrderResponse declineOrder(Long id, DeclineOrderRequest request) {
        log.info("Declining order id {}", id);
        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.AWAITING_APPROVAL) {
            throw new BusinessException("Only orders in AWAITING_APPROVAL status can be declined");
        }

        order.setStatus(OrderStatus.DECLINED);
        order.setDeclineReason(request.getReason());
        Order saved = orderRepository.save(order);
        log.info("Order '{}' declined. Reason: {}", saved.getOrderNumber(), request.getReason());
        return toResponse(saved, Role.WAREHOUSE_MANAGER);
    }

    @Override
    @Transactional
    public OrderResponse fulfillOrder(Long id) {
        log.info("Fulfilling order id {}", id);
        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new BusinessException("Only APPROVED orders can be fulfilled");
        }

        order.setStatus(OrderStatus.FULFILLED);
        Order saved = orderRepository.save(order);
        log.info("Order '{}' fulfilled", saved.getOrderNumber());
        return toResponse(saved, Role.WAREHOUSE_MANAGER);
    }


    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void buildOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        for (OrderItemRequest req : itemRequests) {
            InventoryItem inventoryItem = inventoryItemRepository.findById(req.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory item not found with id: " + req.getInventoryItemId()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .inventoryItem(inventoryItem)
                    .quantity(req.getQuantity())
                    .build();
            order.getItems().add(orderItem);
        }
    }

    private String generateOrderNumber() {
        Integer max = orderRepository.findMaxOrderSequence();
        int next = (max == null ? 0 : max) + 1;
        return String.format("ORD-%05d", next);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void assertOwnership(Order order, String username) {
        if (!order.getClient().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
    }

    private OrderResponse toResponse(Order order, Role callerRole) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(oi -> {
                    BigDecimal lineTotal = oi.getInventoryItem().getUnitPrice()
                            .multiply(BigDecimal.valueOf(oi.getQuantity()));
                    return OrderItemResponse.builder()
                            .id(oi.getId())
                            .inventoryItemId(oi.getInventoryItem().getId())
                            .itemName(oi.getInventoryItem().getItemName())
                            .unitPrice(oi.getInventoryItem().getUnitPrice())
                            .quantity(oi.getQuantity())
                            .lineTotal(lineTotal)
                            .build();
                }).toList();

        BigDecimal total = itemResponses.stream()
                .map(OrderItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AvailableAction> actions = actionResolver.resolve(
                order.getId(), order.getStatus(), callerRole);

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .clientUsername(order.getClient().getUsername())
                .clientFullName(order.getClient().getFullName())
                .status(order.getStatus())
                .submittedDate(order.getSubmittedDate())
                .deadlineDate(order.getDeadlineDate())
                .declineReason(order.getDeclineReason())
                .items(itemResponses)
                .totalAmount(total)
                .availableActions(actions)
                .build();
    }
}
