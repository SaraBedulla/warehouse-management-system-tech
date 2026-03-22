package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeclineOrderRequest {

    @NotBlank(message = "Decline reason is required")
    @Size(max = 500, message = "Decline reason must not exceed 500 characters")
    private String reason;
}
