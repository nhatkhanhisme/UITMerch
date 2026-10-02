package com.uitmerch.backend.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Order metrics use current status and the order's creation date; inventory is current. */
public record AnalyticsResponse(LocalDate from, LocalDate to, Totals orders, Inventory inventory,
        List<ProductSales> topProducts, List<DailyOrders> dailyOrders, List<PickupWorkload> pickupWorkload) {
    public record Totals(long total, long pending, long confirmed, long ready, long completed, long cancelled,
            long completedQuantity, BigDecimal completedOrderValue, BigDecimal activeOrderValue,
            BigDecimal paidOrderValue, BigDecimal cancellationRate) {}
    public record Inventory(long products, long publishedProducts, long availableUnits, long outOfStockProducts) {}
    public record ProductSales(UUID merchId, String name, long quantity, BigDecimal completedOrderValue) {}
    public record DailyOrders(LocalDate date, long orders, long completed, long cancelled, BigDecimal completedOrderValue) {}
    public record PickupWorkload(UUID scheduleId, LocalDate date, String timeSlot, long ready, long completed) {}
}
