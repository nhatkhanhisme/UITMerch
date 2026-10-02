package com.uitmerch.backend.merch.service;

import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.restock.RestockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class InventoryService {
    private final MerchItemRepository merch;
    private final RestockService restock;

    @Transactional
    public void restore(UUID merchId, int quantity) {
        if (quantity < 1) throw new ValidationException("Quantity must be positive.");
        var item = merch.findLockedById(merchId).orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
        int previous = item.getStock();
        item.setStock(Math.addExact(previous, quantity));
        restock.stockIncreased(item, previous);
        merch.save(item);
    }
}
