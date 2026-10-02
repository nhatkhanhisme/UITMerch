package com.uitmerch.backend.analytics;

import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    private final OrganizationService organizations;

    @Transactional(readOnly = true)
    public AnalyticsResponse report(UUID owner, UUID org, LocalDate from, LocalDate to) {
        organizations.getOwnOrganizationEntity(owner, org);
        LocalDate end = to == null ? LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= 366 || end.getYear() >= 9999) {
            throw new ValidationException("Date range must contain between 1 and 366 days.");
        }
        Object[] window = {org, start.atStartOfDay(), end.plusDays(1).atStartOfDay()};
        String scope = " WHERE org_id = ? AND created_at >= ? AND created_at < ? ";
        var totals = jdbc.queryForObject("""
            SELECT COUNT(*) AS total,
              COALESCE(SUM(CASE WHEN status='PENDING' THEN 1 ELSE 0 END),0) AS pending,
              COALESCE(SUM(CASE WHEN status='CONFIRMED' THEN 1 ELSE 0 END),0) AS confirmed,
              COALESCE(SUM(CASE WHEN status='READY' THEN 1 ELSE 0 END),0) AS ready,
              COALESCE(SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END),0) AS completed,
              COALESCE(SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END),0) AS cancelled,
              COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END),0) AS completed_value,
              COALESCE(SUM(CASE WHEN status<>'CANCELLED' THEN total_amount ELSE 0 END),0) AS active_value,
              COALESCE(SUM(CASE WHEN status<>'CANCELLED' AND payment_status='PAID' THEN total_amount ELSE 0 END),0) AS paid_value
            FROM orders
            """ + scope, (rs, i) -> new AnalyticsResponse.Totals(rs.getLong("total"), rs.getLong("pending"),
                rs.getLong("confirmed"), rs.getLong("ready"), rs.getLong("completed"), rs.getLong("cancelled"), 0,
                rs.getBigDecimal("completed_value"), rs.getBigDecimal("active_value"), rs.getBigDecimal("paid_value"),
                rs.getLong("total") == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(rs.getLong("cancelled"))
                    .divide(BigDecimal.valueOf(rs.getLong("total")), 4, RoundingMode.HALF_UP)), window);
        Long quantity = jdbc.queryForObject("""
            SELECT COALESCE(SUM(i.quantity),0) FROM order_items i JOIN orders o ON o.id=i.order_id
            WHERE o.org_id=? AND o.created_at>=? AND o.created_at<? AND o.status='COMPLETED'
            """, Long.class, window);
        totals = new AnalyticsResponse.Totals(totals.total(), totals.pending(), totals.confirmed(), totals.ready(),
            totals.completed(), totals.cancelled(), quantity, totals.completedOrderValue(), totals.activeOrderValue(),
            totals.paidOrderValue(), totals.cancellationRate());
        var inventory = jdbc.queryForObject("""
            SELECT COUNT(*) AS products,
              COALESCE(SUM(CASE WHEN status='PUBLISHED' THEN 1 ELSE 0 END),0) AS published,
              COALESCE(SUM(CASE WHEN status='PUBLISHED' THEN stock ELSE 0 END),0) AS units,
              COALESCE(SUM(CASE WHEN status='PUBLISHED' AND stock=0 THEN 1 ELSE 0 END),0) AS unavailable
            FROM merch_items WHERE org_id=?
            """, (rs, i) -> new AnalyticsResponse.Inventory(rs.getLong("products"), rs.getLong("published"),
                rs.getLong("units"), rs.getLong("unavailable")), org);
        var products = jdbc.query("""
            SELECT i.merch_id, MAX(i.merch_name) AS name, SUM(i.quantity) AS quantity, SUM(i.subtotal) AS value
            FROM order_items i JOIN orders o ON o.id=i.order_id
            WHERE o.org_id=? AND o.created_at>=? AND o.created_at<? AND o.status='COMPLETED'
            GROUP BY i.merch_id ORDER BY quantity DESC, i.merch_id LIMIT 20
            """, (rs, i) -> new AnalyticsResponse.ProductSales(rs.getObject("merch_id", UUID.class),
                rs.getString("name"), rs.getLong("quantity"), rs.getBigDecimal("value")), window);
        var daily = jdbc.query("""
            SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS orders,
              SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed,
              SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled,
              SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END) AS value
            FROM orders
            """ + scope + " GROUP BY CAST(created_at AS DATE) ORDER BY day",
            (rs, i) -> new AnalyticsResponse.DailyOrders(rs.getObject("day", LocalDate.class), rs.getLong("orders"),
                rs.getLong("completed"), rs.getLong("cancelled"), rs.getBigDecimal("value")), window);
        var pickup = jdbc.query("""
            SELECT s.id, s.pickup_date, s.pickup_time_slot,
              SUM(CASE WHEN o.status='READY' THEN 1 ELSE 0 END) AS ready,
              SUM(CASE WHEN o.status='COMPLETED' THEN 1 ELSE 0 END) AS completed
            FROM pickup_schedules s LEFT JOIN orders o ON o.pickup_schedule_id=s.id AND o.org_id=s.org_id
            WHERE s.org_id=? AND s.pickup_date>=? AND s.pickup_date<=?
            GROUP BY s.id, s.pickup_date, s.pickup_time_slot ORDER BY s.pickup_date, s.id
            """, (rs, i) -> new AnalyticsResponse.PickupWorkload(rs.getObject("id", UUID.class),
                rs.getObject("pickup_date", LocalDate.class), rs.getString("pickup_time_slot"), rs.getLong("ready"),
                rs.getLong("completed")), org, start, end);
        return new AnalyticsResponse(start, end, totals, inventory, products, daily, pickup);
    }
}
