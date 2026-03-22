package com.wms.service;

import com.wms.dto.request.InventoryItemRequest;
import com.wms.dto.response.InventoryItemResponse;

import java.util.List;

public interface InventoryService {
    InventoryItemResponse createItem(InventoryItemRequest request);
    InventoryItemResponse getItemById(Long id);
    List<InventoryItemResponse> getAllItems();
    InventoryItemResponse updateItem(Long id, InventoryItemRequest request);
    void deleteItem(Long id);
}
