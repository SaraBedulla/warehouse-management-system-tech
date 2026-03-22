package com.wms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateOrderRequest {

    @Future(message = "Deadline must be a future date")
    private LocalDate deadlineDate;

    private List<OrderItemRequest> items;
}
