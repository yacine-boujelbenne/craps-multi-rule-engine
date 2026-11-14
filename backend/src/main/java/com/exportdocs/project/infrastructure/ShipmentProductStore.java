package com.exportdocs.project.infrastructure;

import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.domain.ShipmentProduct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentProductStore {
    private final JdbcTemplate jdbc;
    public ShipmentProductStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ShipmentProduct> list(UUID project) {
        return jdbc.query("SELECT * FROM project_product WHERE project_id=? ORDER BY created_at,id", this::row, project);
    }

    public ShipmentProduct find(UUID project, UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM project_product WHERE project_id=? AND id=?" + (lock ? " FOR UPDATE" : ""),
            this::row, project, id);
        if (rows.isEmpty()) throw ProjectProblem.field(404, "projectProductId", "not_found", "Shipment product not found.");
        return rows.getFirst();
    }

    public void requireProduct(UUID product) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM product WHERE id=?)", Boolean.class, product)))
            throw ProjectProblem.field(404, "productId", "not_found", "Product not found.");
    }

    public Grade activeGrade(UUID product, UUID category) {
        var rows = jdbc.query("SELECT * FROM product_category WHERE product_id=? AND id=? FOR SHARE", this::grade, product, category);
        if (rows.isEmpty() || !rows.getFirst().active()) throw ProjectProblem.field(400, "productCategoryId", "invalid_reference",
            "Grade must be active and belong to the selected product.");
        return rows.getFirst();
    }

    public ShipmentProduct create(UUID project, ShipmentProduct product) {
        jdbc.update("""
            INSERT INTO project_product(id,project_id,product_id,product_category_id,carton_count,quantity,quantity_unit,
                net_weight_kg,gross_weight_kg,unit_price,currency,lot_reference,processing_information,
                applied_grade_version,applied_grade_code,applied_grade_name,applied_carton_net_kg)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, product.id(), project, product.productId(), product.productCategoryId(), product.cartonCount(),
            product.quantity(), product.quantityUnit(), product.netWeightKg(), product.grossWeightKg(), product.unitPrice(),
            product.currency(), product.lotReference(), product.processingInformation(), product.appliedGradeVersion(),
            product.appliedGradeCode(), product.appliedGradeName(), product.appliedCartonNetKg());
        return find(project, product.id(), false);
    }

    public ShipmentProduct update(UUID project, ShipmentProduct product) {
        jdbc.update("""
            UPDATE project_product SET product_category_id=?,carton_count=?,quantity=?,quantity_unit=?,net_weight_kg=?,
                gross_weight_kg=?,unit_price=?,currency=?,lot_reference=?,processing_information=?,applied_grade_version=?,
                applied_grade_code=?,applied_grade_name=?,applied_carton_net_kg=?,version=version+1,
                updated_at=greatest(clock_timestamp(),updated_at+interval '1 microsecond') WHERE project_id=? AND id=?
            """, product.productCategoryId(), product.cartonCount(), product.quantity(), product.quantityUnit(),
            product.netWeightKg(), product.grossWeightKg(), product.unitPrice(), product.currency(), product.lotReference(),
            product.processingInformation(), product.appliedGradeVersion(), product.appliedGradeCode(), product.appliedGradeName(),
            product.appliedCartonNetKg(), project, product.id());
        return find(project, product.id(), false);
    }

    private ShipmentProduct row(ResultSet rs, int index) throws SQLException {
        return new ShipmentProduct(rs.getObject("id", UUID.class), rs.getLong("version"), rs.getObject("product_id", UUID.class),
            rs.getObject("product_category_id", UUID.class), (Integer) rs.getObject("carton_count"), rs.getBigDecimal("quantity"),
            rs.getString("quantity_unit"), rs.getBigDecimal("net_weight_kg"), rs.getBigDecimal("gross_weight_kg"),
            rs.getBigDecimal("unit_price"), rs.getString("currency"), rs.getString("lot_reference"),
            rs.getString("processing_information"), (Long) rs.getObject("applied_grade_version"),
            rs.getString("applied_grade_code"), rs.getString("applied_grade_name"), rs.getBigDecimal("applied_carton_net_kg"));
    }

    private Grade grade(ResultSet rs, int index) throws SQLException {
        return new Grade(rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class), rs.getLong("version"),
            rs.getString("code"), rs.getString("name"), rs.getInt("display_order"), rs.getBigDecimal("default_carton_net_kg"),
            rs.getBigDecimal("default_price_per_kg"), rs.getString("default_currency"), rs.getBoolean("active"));
    }
}
