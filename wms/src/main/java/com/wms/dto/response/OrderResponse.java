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
    private List<AvailableAction> availableActions;
    private int attachmentCount;
}

