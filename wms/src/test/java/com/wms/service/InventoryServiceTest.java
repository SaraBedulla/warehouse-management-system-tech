package com.wms.service;

import com.wms.dto.request.InventoryItemRequest;
import com.wms.dto.response.InventoryItemResponse;
import com.wms.entity.InventoryItem;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.InventoryItemRepository;
import com.wms.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService")
class InventoryServiceTest {

    @Mock InventoryItemRepository inventoryItemRepository;
    @InjectMocks InventoryServiceImpl inventoryService;

    private InventoryItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = InventoryItem.builder()
                .id(1L).itemName("Cardboard Box")
                .quantity(100).unitPrice(new BigDecimal("1.50")).build();
    }

    // ─── createItem ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createItem")
    class CreateItem {

        @Test
        @DisplayName("creates item when name is unique")
        void success() {
            InventoryItemRequest req = new InventoryItemRequest();
            req.setItemName("Safety Gloves"); req.setQuantity(200);
            req.setUnitPrice(new BigDecimal("4.99"));

            when(inventoryItemRepository.existsByItemNameIgnoreCase("Safety Gloves")).thenReturn(false);
            when(inventoryItemRepository.save(any())).thenAnswer(i -> {
                InventoryItem item = i.getArgument(0); item.setId(2L); return item;
            });

            InventoryItemResponse res = inventoryService.createItem(req);

            assertThat(res.getItemName()).isEqualTo("Safety Gloves");
            assertThat(res.getQuantity()).isEqualTo(200);
            assertThat(res.getUnitPrice()).isEqualByComparingTo("4.99");
        }

        @Test
        @DisplayName("throws BusinessException when name already exists")
        void duplicateName_throws() {
            InventoryItemRequest req = new InventoryItemRequest();
            req.setItemName("Cardboard Box"); req.setQuantity(10);
            req.setUnitPrice(BigDecimal.ONE);

            when(inventoryItemRepository.existsByItemNameIgnoreCase("Cardboard Box")).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.createItem(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // ─── getItemById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getItemById")
    class GetItemById {

        @Test
        @DisplayName("returns item when found")
        void found() {
            when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
            InventoryItemResponse res = inventoryService.getItemById(1L);
            assertThat(res.getItemName()).isEqualTo("Cardboard Box");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void notFound() {
            when(inventoryItemRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> inventoryService.getItemById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAllItems ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllItems returns all items mapped to response")
    void getAllItems() {
        when(inventoryItemRepository.findAll()).thenReturn(List.of(sampleItem));
        List<InventoryItemResponse> result = inventoryService.getAllItems();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemName()).isEqualTo("Cardboard Box");
    }

    // ─── updateItem ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateItem")
    class UpdateItem {

        @Test
        @DisplayName("updates item successfully when name is unchanged")
        void success_sameName() {
            InventoryItemRequest req = new InventoryItemRequest();
            req.setItemName("Cardboard Box"); req.setQuantity(200);
            req.setUnitPrice(new BigDecimal("2.00"));

            when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
            when(inventoryItemRepository.save(any())).thenReturn(sampleItem);

            InventoryItemResponse res = inventoryService.updateItem(1L, req);

            assertThat(sampleItem.getQuantity()).isEqualTo(200);
            assertThat(sampleItem.getUnitPrice()).isEqualByComparingTo("2.00");
        }

        @Test
        @DisplayName("throws BusinessException when new name conflicts with existing item")
        void duplicateName_throws() {
            InventoryItemRequest req = new InventoryItemRequest();
            req.setItemName("Bubble Wrap"); req.setQuantity(50);
            req.setUnitPrice(BigDecimal.ONE);

            when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
            when(inventoryItemRepository.existsByItemNameIgnoreCase("Bubble Wrap")).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.updateItem(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // ─── deleteItem ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteItem removes the item")
    void deleteItem() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        inventoryService.deleteItem(1L);
        verify(inventoryItemRepository).delete(sampleItem);
    }
}
