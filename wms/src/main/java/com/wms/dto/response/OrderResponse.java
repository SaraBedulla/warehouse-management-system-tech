package com.wms.dto.response;

import com.wms.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String clientUsername;
    private String clientFullName;
    private OrderStatus status;
    private LocalDateTime submittedDate;
    private LocalDate deadlineDate;
    private String declineReason;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;

    /**
     * Dynamic list of actions available to the requesting user for this order.
     * Each action includes everything the frontend needs to render a button:
     * label, endpoint, HTTP method, color, and whether a request body is needed.
     */
    private List<AvailableAction> availableActions;
}
