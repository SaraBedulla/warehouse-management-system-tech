package com.wms.service;

import com.wms.dto.response.AvailableAction;
import com.wms.enums.OrderAction;
import com.wms.enums.OrderStatus;
import com.wms.enums.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the list of available actions for an order based on its current
 * status and the role of the requesting user.
 *
 * Keeping this logic here (rather than in OrderServiceImpl) means:
 *  - The mapping is easy to find, read, and change in one place.
 *  - OrderServiceImpl stays focused on mutations.
 *  - The resolver can be unit-tested in isolation with no DB involvement.
 */
@Service
public class OrderActionResolver {

    public List<AvailableAction> resolve(Long orderId, OrderStatus status, Role role) {
        return switch (role) {
            case CLIENT -> resolveForClient(orderId, status);
            case WAREHOUSE_MANAGER -> resolveForManager(orderId, status);
            case SYSTEM_ADMIN -> List.of(); // admins manage users, not orders
        };
    }

    // ─── CLIENT ──────────────────────────────────────────────────────────────

    private List<AvailableAction> resolveForClient(Long orderId, OrderStatus status) {
        List<AvailableAction> actions = new ArrayList<>();

        switch (status) {
            case CREATED -> {
                actions.add(edit(orderId));
                actions.add(submit(orderId));
                actions.add(cancel(orderId));
            }
            case DECLINED -> {
                // Client can fix the order and resubmit, or cancel it
                actions.add(edit(orderId));
                actions.add(submit(orderId));
                actions.add(cancel(orderId));
            }
            case AWAITING_APPROVAL -> {
                // Client is waiting — only option is to pull it back
                actions.add(cancel(orderId));
            }
            case APPROVED -> {
                // Approved — client can still cancel before fulfillment
                actions.add(cancel(orderId));
            }
            case FULFILLED, CANCELED -> {
                // Terminal states — no further actions
            }
        }

        return actions;
    }

    // ─── WAREHOUSE MANAGER ───────────────────────────────────────────────────

    private List<AvailableAction> resolveForManager(Long orderId, OrderStatus status) {
        List<AvailableAction> actions = new ArrayList<>();

        switch (status) {
            case AWAITING_APPROVAL -> {
                actions.add(approve(orderId));
                actions.add(decline(orderId));
            }
            case APPROVED -> {
                actions.add(fulfill(orderId));
            }
            case CREATED, DECLINED, FULFILLED, CANCELED -> {
                // Nothing actionable for manager in these states
            }
        }

        return actions;
    }

    // ─── Action builders ─────────────────────────────────────────────────────

    private AvailableAction edit(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.EDIT)
                .label("Edit Order")
                .endpoint("/api/orders/" + orderId)
                .method("PUT")
                .color("secondary")
                .requiresBody(true)
                .build();
    }

    private AvailableAction submit(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.SUBMIT)
                .label("Submit for Approval")
                .endpoint("/api/orders/" + orderId + "/submit")
                .method("POST")
                .color("primary")
                .requiresBody(false)
                .build();
    }

    private AvailableAction cancel(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.CANCEL)
                .label("Cancel Order")
                .endpoint("/api/orders/" + orderId + "/cancel")
                .method("POST")
                .color("danger")
                .requiresBody(false)
                .build();
    }

    private AvailableAction approve(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.APPROVE)
                .label("Approve")
                .endpoint("/api/manager/orders/" + orderId + "/approve")
                .method("POST")
                .color("success")
                .requiresBody(false)
                .build();
    }

    private AvailableAction decline(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.DECLINE)
                .label("Decline")
                .endpoint("/api/manager/orders/" + orderId + "/decline")
                .method("POST")
                .color("danger")
                .requiresBody(true)  // needs a DeclineOrderRequest body with reason
                .build();
    }

    private AvailableAction fulfill(Long orderId) {
        return AvailableAction.builder()
                .action(OrderAction.FULFILL)
                .label("Mark as Fulfilled")
                .endpoint("/api/manager/orders/" + orderId + "/fulfill")
                .method("POST")
                .color("success")
                .requiresBody(false)
                .build();
    }
}
