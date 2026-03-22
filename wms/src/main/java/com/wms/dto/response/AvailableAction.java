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

    /** Semantic action identifier — frontend can use this for routing logic */
    private OrderAction action;

    /** Human-readable button label */
    private String label;

    /** Relative endpoint to call — includes the order id */
    private String endpoint;

    /** HTTP method for the call */
    private String method;

    /**
     * Semantic color hint for the frontend.
     * Values: "primary" | "success" | "danger" | "warning" | "secondary"
     */
    private String color;

    /**
     * Whether this action requires a request body (e.g. DECLINE needs a reason).
     * The frontend can use this to show/hide an input form before calling.
     */
    private boolean requiresBody;
}
