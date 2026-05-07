package com.uitmerch.backend.collections.event;

import com.uitmerch.backend.collections.service.CollectionService;
import com.uitmerch.backend.orders.event.OrderSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for successful order transitions and creates collection rows after commit.
 */
@Component
public class OrderSucceededEventListener {

    private final CollectionService collectionService;

    public OrderSucceededEventListener(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSucceeded(OrderSucceededEvent event) {
        collectionService.createCollectionsForSuccessfulOrder(event.orderId());
    }
}