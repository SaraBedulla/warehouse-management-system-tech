package com.wms.service;

import com.wms.dto.request.CreateOrderRequest;
import com.wms.dto.request.DeclineOrderRequest;
import com.wms.dto.request.OrderItemRequest;
import com.wms.dto.response.OrderResponse;
import com.wms.entity.InventoryItem;
import com.wms.entity.Order;
import com.wms.entity.User;
import com.wms.enums.OrderStatus;
import com.wms.enums.Role;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.InventoryItemRepository;
import com.wms.repository.OrderRepository;
import com.wms.repository.UserRepository;
import com.wms.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock OrderActionResolver actionResolver;
    @InjectMocks OrderServiceImpl orderService;

    private User client;
    private Order createdOrder;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        client = User.builder()
                .id(1L).username("client1").fullName("Alice")
                .email("alice@wms.com").role(Role.CLIENT)
                .password("hashed").enabled(true).build();

        inventoryItem = InventoryItem.builder()
                .id(1L).itemName("Box").quantity(100)
                .unitPrice(new BigDecimal("5.00")).build();

        createdOrder = Order.builder()
                .id(1L).orderNumber("ORD-00001")
                .client(client).status(OrderStatus.CREATED)
                .items(new ArrayList<>()).build();

        when(actionResolver.resolve(any(), any(), any())).thenReturn(List.of());
    }

    // ─── createOrder ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("creates order in CREATED status")
        void success() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setItems(List.of());

            when(userRepository.findByUsername("client1")).thenReturn(Optional.of(client));
            when(orderRepository.findMaxOrderSequence()).thenReturn(0);
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.createOrder(req, "client1");

            assertThat(res.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(res.getOrderNumber()).isEqualTo("ORD-00001");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user not found")
        void userNotFound_throws() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(), "ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── submitOrder ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitOrder")
    class SubmitOrder {

        @Test
        @DisplayName("moves order to AWAITING_APPROVAL")
        void success() {
            // Add one item so order is not empty
            com.wms.entity.OrderItem oi = com.wms.entity.OrderItem.builder()
                    .id(1L).order(createdOrder).inventoryItem(inventoryItem).quantity(2).build();
            createdOrder.getItems().add(oi);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.submitOrder(1L, "client1");

            assertThat(res.getStatus()).isEqualTo(OrderStatus.AWAITING_APPROVAL);
        }

        @Test
        @DisplayName("throws BusinessException when order has no items")
        void emptyOrder_throws() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.submitOrder(1L, "client1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no items");
        }

        @Test
        @DisplayName("throws BusinessException when status is not CREATED")
        void wrongStatus_throws() {
            createdOrder.setStatus(OrderStatus.AWAITING_APPROVAL);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.submitOrder(1L, "client1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when caller is not the owner")
        void notOwner_throws() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.submitOrder(1L, "other_user"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─── cancelOrder ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("cancels order successfully from CREATED status")
        void success() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.cancelOrder(1L, "client1");

            assertThat(res.getStatus()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("throws BusinessException when order is already FULFILLED")
        void fulfilled_throws() {
            createdOrder.setStatus(OrderStatus.FULFILLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, "client1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("FULFILLED");
        }

        @Test
        @DisplayName("throws BusinessException when order is already CANCELED")
        void alreadyCanceled_throws() {
            createdOrder.setStatus(OrderStatus.CANCELED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, "client1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CANCELED");
        }
    }

    // ─── approveOrder ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveOrder")
    class ApproveOrder {

        @Test
        @DisplayName("moves order from AWAITING_APPROVAL to APPROVED")
        void success() {
            createdOrder.setStatus(OrderStatus.AWAITING_APPROVAL);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.approveOrder(1L);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.APPROVED);
        }

        @Test
        @DisplayName("throws BusinessException when order is not AWAITING_APPROVAL")
        void wrongStatus_throws() {
            createdOrder.setStatus(OrderStatus.CREATED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.approveOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AWAITING_APPROVAL");
        }
    }

    // ─── declineOrder ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("declineOrder")
    class DeclineOrder {

        @Test
        @DisplayName("moves order to DECLINED with reason")
        void success() {
            createdOrder.setStatus(OrderStatus.AWAITING_APPROVAL);
            DeclineOrderRequest req = new DeclineOrderRequest();
            req.setReason("Out of stock");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.declineOrder(1L, req);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.DECLINED);
            assertThat(createdOrder.getDeclineReason()).isEqualTo("Out of stock");
        }

        @Test
        @DisplayName("throws BusinessException when order is not AWAITING_APPROVAL")
        void wrongStatus_throws() {
            createdOrder.setStatus(OrderStatus.APPROVED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            DeclineOrderRequest req = new DeclineOrderRequest();
            req.setReason("reason");

            assertThatThrownBy(() -> orderService.declineOrder(1L, req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── fulfillOrder ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fulfillOrder")
    class FulfillOrder {

        @Test
        @DisplayName("moves order from APPROVED to FULFILLED")
        void success() {
            createdOrder.setStatus(OrderStatus.APPROVED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));
            when(orderRepository.save(any())).thenReturn(createdOrder);

            OrderResponse res = orderService.fulfillOrder(1L);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.FULFILLED);
        }

        @Test
        @DisplayName("throws BusinessException when order is not APPROVED")
        void wrongStatus_throws() {
            createdOrder.setStatus(OrderStatus.AWAITING_APPROVAL);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.fulfillOrder(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("APPROVED");
        }
    }

    // ─── getMyOrders ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyOrders returns orders filtered by client")
    void getMyOrders() {
        when(userRepository.findByUsername("client1")).thenReturn(Optional.of(client));
        when(orderRepository.findByClient(client)).thenReturn(List.of(createdOrder));

        List<OrderResponse> result = orderService.getMyOrders("client1", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-00001");
    }

    @Test
    @DisplayName("getMyOrders filters by status when provided")
    void getMyOrders_withStatus() {
        when(userRepository.findByUsername("client1")).thenReturn(Optional.of(client));
        when(orderRepository.findByClientAndStatus(client, OrderStatus.CREATED)).thenReturn(List.of(createdOrder));

        List<OrderResponse> result = orderService.getMyOrders("client1", OrderStatus.CREATED);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByClientAndStatus(client, OrderStatus.CREATED);
    }
}
