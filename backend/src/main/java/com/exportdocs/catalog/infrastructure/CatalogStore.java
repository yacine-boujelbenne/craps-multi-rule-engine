package com.exportdocs.catalog.infrastructure;

import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.domain.Product;
import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.application.ProjectProblem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogStore {
    private final JdbcTemplate jdbc;

    public CatalogStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Product product(UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM product WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::productRow, id);
        if (rows.isEmpty()) throw ProjectProblem.notFound();
        return rows.getFirst();
    }

    public Grade grade(UUID productId, UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM product_category WHERE product_id=? AND id=?" + (lock ? " FOR UPDATE" : ""), this::gradeRow, productId,
            id);
        if (rows.isEmpty()) throw ProjectProblem.notFound();
        return rows.getFirst();
    }

    public Product createProduct(UUID id, ProductWrite.Fields f) {
        try {
            jdbc.update("INSERT INTO product(id,code,name,scientific_name,hs_code,active) VALUES (?,?,?,?,?,?)", id, f.code(), f.name(), f.scientificName(), f.hsCode(), f.active());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return product(id, false);
    }

    public Product updateProduct(Product p) {
        try {
            jdbc.update("UPDATE product SET code=?,name=?,scientific_name=?,hs_code=?,active=?,version=version+1 WHERE id=?", p.code(), p.name(), p.scientificName(), p.hsCode(), p.active(), p.id());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return product(p.id(), false);
    }

    public Grade createGrade(UUID productId, UUID id, GradeWrite.Fields f) {
        try {
            jdbc.update("""
                INSERT INTO product_category(id,product_id,code,name,display_order,default_carton_net_kg,default_price_per_kg,default_currency,active)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, id, productId, f.code(), f.name(), f.displayOrder(), f.defaultCartonNetKg(),
                f.defaultPricePerKg(), f.defaultCurrency(), f.active());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return grade(productId, id, false);
    }

    public Grade updateGrade(Grade g) {
        try {
            jdbc.update("""
                UPDATE product_category SET code=?,name=?,display_order=?,default_carton_net_kg=?,default_price_per_kg=?,default_currency=?,active=?,version=version+1
                WHERE id=? AND product_id=?
                """, g.code(), g.name(), g.displayOrder(), g.defaultCartonNetKg(), g.defaultPricePerKg(),
                g.defaultCurrency(), g.active(), g.id(), g.productId());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return grade(g.productId(), g.id(), false);
    }

    public ProductPage products(SetupListQuery query) {
        var args = new ArrayList<Object>();
        var where = filters(query, args);
        long total = jdbc.queryForObject("SELECT count(*) FROM product" + where, Long.class, args.toArray());
        args.add(query.size());
        args.add((long)query.page()*query.size());
        return new ProductPage(jdbc.query("SELECT * FROM product" + where + " ORDER BY name,id LIMIT ? OFFSET ?", this::productRow, args.toArray()),
            total);
    }

    public GradePage grades(UUID product, SetupListQuery query) {
        var args = new ArrayList<Object>();
        var where = filters(query, args) + " AND product_id=?";
        args.add(product);
        long total = jdbc.queryForObject("SELECT count(*) FROM product_category" + where, Long.class, args.toArray());
        args.add(query.size());
        args.add((long)query.page()*query.size());
        return new GradePage(jdbc.query("SELECT * FROM product_category" + where + " ORDER BY display_order,code,id LIMIT ? OFFSET ?", this::gradeRow, args.toArray()),
            total);
    }

    private String filters(SetupListQuery query, List<Object> args) {
        String where = " WHERE 1=1";
        if (!query.includeInactive())where+=" AND active";
        if (query.prefix() != null) {
            where+=" AND (lower(code) LIKE ? ESCAPE '!' OR lower(name) LIKE ? ESCAPE '!')";
            args.add(query.prefix());
            args.add(query.prefix());
        }
        return where;
    }

    private Product productRow(ResultSet rs, int row) throws SQLException {
        return new Product(rs.getObject("id", UUID.class), rs.getLong("version"),
            rs.getString("code"), rs.getString("name"), rs.getString("scientific_name"),
            rs.getString("hs_code"), rs.getBoolean("active"));
    }

    private Grade gradeRow(ResultSet rs, int row) throws SQLException {
        return new Grade(rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class),
            rs.getLong("version"), rs.getString("code"), rs.getString("name"),
            rs.getInt("display_order"), rs.getBigDecimal("default_carton_net_kg"), rs.getBigDecimal("default_price_per_kg"),
            rs.getString("default_currency"), rs.getBoolean("active"));
    }

    private RuntimeException duplicateOrOriginal(DataIntegrityViolationException exception) {
        if (OrganizationStore.sqlState(exception, "23505")) return ProjectProblem.field(409,
            "code", "duplicate_code", "Code already exists within this catalog scope.");
        return exception;
    }

    public record ProductPage(List<Product> items, long total) {
    }

    public record GradePage(List<Grade> items, long total) {
    }
}
