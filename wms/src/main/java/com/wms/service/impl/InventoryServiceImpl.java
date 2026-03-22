package com.wms.service.impl;

import com.wms.dto.request.InventoryItemRequest;
import com.wms.dto.response.InventoryItemResponse;
import com.wms.entity.InventoryItem;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.InventoryItemRepository;
import com.wms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LogManager.getLogger(InventoryServiceImpl.class);

    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest request) {
        log.info("Creating inventory item: {}", request.getItemName());

        if (inventoryItemRepository.existsByItemNameIgnoreCase(request.getItemName())) {
            throw new BusinessException("Inventory item '" + request.getItemName() + "' already exists");
        }

        InventoryItem item = InventoryItem.builder()
                .itemName(request.getItemName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .build();

        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Inventory item created with id {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getItemById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getAllItems() {
        return inventoryItemRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, InventoryItemRequest request) {
        log.info("Updating inventory item id: {}", id);
        InventoryItem item = findById(id);

        // Check name uniqueness if changed
        if (!item.getItemName().equalsIgnoreCase(request.getItemName())
                && inventoryItemRepository.existsByItemNameIgnoreCase(request.getItemName())) {
            throw new BusinessException("Inventory item '" + request.getItemName() + "' already exists");
        }

        item.setItemName(request.getItemName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());

        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Inventory item '{}' updated", saved.getItemName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        log.info("Deleting inventory item id: {}", id);
        InventoryItem item = findById(id);
        inventoryItemRepository.delete(item);
        log.info("Inventory item '{}' deleted", item.getItemName());
    }

    private InventoryItem findById(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));
    }

    public InventoryItemResponse toResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
