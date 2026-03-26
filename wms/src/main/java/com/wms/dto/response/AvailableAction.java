package com.wms.dto.response;

import com.wms.enums.OrderAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableAction {

    private OrderAction action;

    private String label;

    private String endpoint;

    private String method;

    private String color;

    private boolean requiresBody;
}
