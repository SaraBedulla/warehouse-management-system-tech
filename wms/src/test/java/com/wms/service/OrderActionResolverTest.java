package com.wms.service;

import com.wms.dto.response.AvailableAction;
import com.wms.enums.OrderAction;
import com.wms.enums.OrderStatus;
import com.wms.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderActionResolver")
class OrderActionResolverTest {

    private OrderActionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OrderActionResolver();
    }

    // ─── CLIENT ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Client actions")
    class ClientActions {

        @Test
        @DisplayName("CREATED → EDIT, SUBMIT, CANCEL")
        void created_returnsEditSubmitCancel() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.CREATED, Role.CLIENT);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.EDIT, OrderAction.SUBMIT, OrderAction.CANCEL);
        }

        @Test
        @DisplayName("DECLINED → EDIT, SUBMIT, CANCEL")
        void declined_returnsEditSubmitCancel() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.DECLINED, Role.CLIENT);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.EDIT, OrderAction.SUBMIT, OrderAction.CANCEL);
        }

        @Test
        @DisplayName("AWAITING_APPROVAL → CANCEL only")
        void awaitingApproval_returnsCancelOnly() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.AWAITING_APPROVAL, Role.CLIENT);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.CANCEL);
        }

        @Test
        @DisplayName("APPROVED → CANCEL only")
        void approved_returnsCancelOnly() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.APPROVED, Role.CLIENT);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.CANCEL);
        }

        @Test
        @DisplayName("FULFILLED → no actions")
        void fulfilled_returnsEmpty() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.FULFILLED, Role.CLIENT);
            assertThat(actions).isEmpty();
        }

        @Test
        @DisplayName("CANCELED → no actions")
        void canceled_returnsEmpty() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.CANCELED, Role.CLIENT);
            assertThat(actions).isEmpty();
        }
    }

    // ─── WAREHOUSE MANAGER ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Manager actions")
    class ManagerActions {

        @Test
        @DisplayName("AWAITING_APPROVAL → APPROVE, DECLINE")
        void awaitingApproval_returnsApproveAndDecline() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.AWAITING_APPROVAL, Role.WAREHOUSE_MANAGER);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.APPROVE, OrderAction.DECLINE);
        }

        @Test
        @DisplayName("APPROVED → FULFILL only")
        void approved_returnsFulfill() {
            List<AvailableAction> actions = resolver.resolve(1L, OrderStatus.APPROVED, Role.WAREHOUSE_MANAGER);

            assertThat(actions).extracting(AvailableAction::getAction)
                    .containsExactly(OrderAction.FULFILL);
        }

        @Test
        @DisplayName("CREATED → no actions for manager")
        void created_returnsEmpty() {
            assertThat(resolver.resolve(1L, OrderStatus.CREATED, Role.WAREHOUSE_MANAGER)).isEmpty();
        }

        @Test
        @DisplayName("DECLINED → no actions for manager")
        void declined_returnsEmpty() {
            assertThat(resolver.resolve(1L, OrderStatus.DECLINED, Role.WAREHOUSE_MANAGER)).isEmpty();
        }

        @Test
        @DisplayName("FULFILLED → no actions for manager")
        void fulfilled_returnsEmpty() {
            assertThat(resolver.resolve(1L, OrderStatus.FULFILLED, Role.WAREHOUSE_MANAGER)).isEmpty();
        }

        @Test
        @DisplayName("CANCELED → no actions for manager")
        void canceled_returnsEmpty() {
            assertThat(resolver.resolve(1L, OrderStatus.CANCELED, Role.WAREHOUSE_MANAGER)).isEmpty();
        }
    }

    // ─── SYSTEM ADMIN ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Admin actions")
    class AdminActions {

        @Test
        @DisplayName("Admin has no order actions regardless of status")
        void admin_alwaysReturnsEmpty() {
            for (OrderStatus status : OrderStatus.values()) {
                assertThat(resolver.resolve(1L, status, Role.SYSTEM_ADMIN))
                        .as("Expected no actions for SYSTEM_ADMIN on status %s", status)
                        .isEmpty();
            }
        }
    }

    // ─── Endpoint and metadata ────────────────────────────────────────────────

    @Nested
    @DisplayName("Action metadata")
    class ActionMetadata {

        @Test
        @DisplayName("SUBMIT action has correct endpoint and method")
        void submit_hasCorrectEndpoint() {
            AvailableAction submit = resolver.resolve(5L, OrderStatus.CREATED, Role.CLIENT)
                    .stream().filter(a -> a.getAction() == OrderAction.SUBMIT).findFirst().orElseThrow();

            assertThat(submit.getEndpoint()).isEqualTo("/api/orders/5/submit");
            assertThat(submit.getMethod()).isEqualTo("POST");
            assertThat(submit.isRequiresBody()).isFalse();
            assertThat(submit.getColor()).isEqualTo("primary");
        }

        @Test
        @DisplayName("DECLINE action requires body")
        void decline_requiresBody() {
            AvailableAction decline = resolver.resolve(3L, OrderStatus.AWAITING_APPROVAL, Role.WAREHOUSE_MANAGER)
                    .stream().filter(a -> a.getAction() == OrderAction.DECLINE).findFirst().orElseThrow();

            assertThat(decline.isRequiresBody()).isTrue();
            assertThat(decline.getEndpoint()).isEqualTo("/api/manager/orders/3/decline");
        }

        @Test
        @DisplayName("EDIT action uses PUT method")
        void edit_usesPutMethod() {
            AvailableAction edit = resolver.resolve(2L, OrderStatus.CREATED, Role.CLIENT)
                    .stream().filter(a -> a.getAction() == OrderAction.EDIT).findFirst().orElseThrow();

            assertThat(edit.getMethod()).isEqualTo("PUT");
            assertThat(edit.isRequiresBody()).isTrue();
        }
    }
}
