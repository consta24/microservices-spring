package com.constams.inventoryservice.service;

import com.constams.inventoryservice.dto.InventoryResponse;
import com.constams.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> inStock(List<String> skuCodes) {
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(inventory ->
                        InventoryResponse.builder()
                                .skuCode(inventory.getSkuCode())
                                .inStock(inventory.getQuantity() > 0)
                                .build()
                )
                .toList();
    }
}
