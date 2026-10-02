package com.uitmerch.backend.features;

import com.uitmerch.backend.analytics.AnalyticsService;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.order.entity.*;
import com.uitmerch.backend.order.repository.*;
import com.uitmerch.backend.organization.entity.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL", matches="jdbc:postgresql:.*")
class AnalyticsFeatureTest extends BackendFeatureTest {
    @Autowired AnalyticsService analytics;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired PickupScheduleRepository schedules;
    @Autowired JdbcTemplate jdbc;
    private Order order(Organization org, OrderStatus status, PaymentStatus payment, String value, LocalDateTime created) {
        var order = orders.saveAndFlush(Order.builder().orgId(org.getId()).userId(user(UserRole.CUSTOMER).getId())
            .totalAmount(new BigDecimal(value)).status(status).paymentStatus(payment).build());
        jdbc.update("UPDATE orders SET created_at=? WHERE id=?", created, order.getId()); return order;
    }
    private void item(Order order, UUID merch, int quantity, String value) {
        items.saveAndFlush(OrderItem.builder().orderId(order.getId()).merchId(merch).merchName("Snapshot")
            .unitPrice(new BigDecimal(value).divide(BigDecimal.valueOf(quantity))).quantity(quantity).subtotal(new BigDecimal(value)).build());
    }
    @Test void aggregatesDoNotDoubleCountItemsOrTreatCompletedOrdersAsPaid() {
        var org=organization(); var date=LocalDate.of(2026,9,15); var a=product(org,4); var b=product(org,0);
        var completed=order(org,OrderStatus.COMPLETED,PaymentStatus.PENDING,"300000",date.atStartOfDay());
        item(completed,a.getId(),2,"200000"); item(completed,b.getId(),1,"100000");
        order(org,OrderStatus.CONFIRMED,PaymentStatus.PAID,"50000",date.atTime(23,59,59));
        order(org,OrderStatus.CANCELLED,PaymentStatus.PAID,"900000",date.atTime(12,0));
        order(organization(),OrderStatus.COMPLETED,PaymentStatus.PAID,"9000000",date.atStartOfDay());
        var report=analytics.report(org.getOwnerId(),org.getId(),date,date);
        assertThat(report.orders().total()).isEqualTo(3);
        assertThat(report.orders().completedQuantity()).isEqualTo(3);
        assertThat(report.orders().completedOrderValue()).isEqualByComparingTo("300000");
        assertThat(report.orders().activeOrderValue()).isEqualByComparingTo("350000");
        assertThat(report.orders().paidOrderValue()).isEqualByComparingTo("50000");
        assertThat(report.orders().cancellationRate()).isEqualByComparingTo("0.3333");
        assertThat(report.inventory().availableUnits()).isEqualTo(4);
        assertThat(report.inventory().outOfStockProducts()).isEqualTo(1);
        assertThat(report.topProducts()).hasSize(2);
        assertThat(report.dailyOrders()).hasSize(1);
        assertThat(report.dailyOrders().getFirst().completedOrderValue()).isEqualByComparingTo("300000");
    }
    @Test void inclusiveDaysExcludeAdjacentOrdersAndPickupUsesScheduleDate() {
        var org=organization(); var date=LocalDate.of(2026,9,16);
        order(org,OrderStatus.PENDING,PaymentStatus.PENDING,"10",date.minusDays(1).atTime(23,59,59));
        order(org,OrderStatus.PENDING,PaymentStatus.PENDING,"20",date.plusDays(1).atStartOfDay());
        var ready=order(org,OrderStatus.READY,PaymentStatus.PENDING,"30",date.minusDays(4).atStartOfDay());
        var slot=schedules.saveAndFlush(PickupSchedule.builder().orgId(org.getId()).pickupDate(date).pickupTimeSlot("08:00-09:00").location("UIT").build());
        ready.setPickupScheduleId(slot.getId()); orders.saveAndFlush(ready);
        var report=analytics.report(org.getOwnerId(),org.getId(),date,date);
        assertThat(report.orders().total()).isZero();
        assertThat(report.pickupWorkload()).hasSize(1);
        assertThat(report.pickupWorkload().getFirst().ready()).isEqualTo(1);
    }
    @Test void rangeIsBoundedAndEmptyOrganizationReturnsZeros() {
        var org=organization(); var date=LocalDate.now();
        var report=analytics.report(org.getOwnerId(),org.getId(),null,null);
        assertThat(report.orders().total()).isZero();
        assertThat(report.orders().paidOrderValue()).isEqualByComparingTo("0");
        assertThatThrownBy(()->analytics.report(org.getOwnerId(),org.getId(),date,date.minusDays(1))).isInstanceOf(com.uitmerch.backend.common.exception.ValidationException.class);
        assertThatThrownBy(()->analytics.report(org.getOwnerId(),org.getId(),date.minusDays(366),date)).isInstanceOf(com.uitmerch.backend.common.exception.ValidationException.class);
    }
    @Test void apiRequiresOrganizerOwnership() throws Exception {
        var org=organization(); String path="/api/v1/organizations/"+org.getId()+"/analytics";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization","Bearer "+token(user(UserRole.CUSTOMER)))).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization","Bearer "+token(user(UserRole.ORGANIZER)))).andExpect(status().isNotFound());
        mvc.perform(get(path).header("Authorization","Bearer "+token(users.findById(org.getOwnerId()).orElseThrow())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.orders.total").value(0));
    }
}
