package com.exportdocs.catalog.infrastructure;

import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.application.TaxonomyWrite;
import com.exportdocs.catalog.domain.Product;
import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.catalog.domain.TaxonomyCategory;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.application.ProjectProblem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            jdbc.update("""
                INSERT INTO product(id,code,name,scientific_name,hs_code,active,description,processing_method,storage_requirements,default_measurement_unit)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, id, f.code(), f.name(), f.scientificName(), f.hsCode(), f.active(), f.description(),
                f.processingMethod(), f.storageRequirements(), f.defaultMeasurementUnit());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return product(id, false);
    }

    public Product updateProduct(Product p) {
        try {
            jdbc.update("""
                UPDATE product SET code=?,name=?,scientific_name=?,hs_code=?,active=?,description=?,processing_method=?,
                    storage_requirements=?,default_measurement_unit=?,version=version+1 WHERE id=?
                """, p.code(), p.name(), p.scientificName(), p.hsCode(), p.active(), p.description(),
                p.processingMethod(), p.storageRequirements(), p.defaultMeasurementUnit(), p.id());
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

    public ProductPage products(UUID category, SetupListQuery query) {
        var args = new ArrayList<Object>();
        var where = filters(query, args);
        if (category != null) {
            where += " AND EXISTS (SELECT 1 FROM product_taxonomy_assignment a WHERE a.product_id=product.id AND a.category_id=?)";
            args.add(category);
        }
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

    public TaxonomyCategory taxonomy(UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM product_taxonomy_category WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::taxonomyRow, id);
        if (rows.isEmpty()) throw ProjectProblem.field(404, "categoryId", "not_found", "Taxonomy category not found.");
        return rows.getFirst();
    }

    public TaxonomyPage taxonomy(SetupListQuery query) {
        var args = new ArrayList<Object>();
        String where = filters(query, args);
        long total = jdbc.queryForObject("SELECT count(*) FROM product_taxonomy_category" + where, Long.class, args.toArray());
        args.add(query.size());
        args.add((long)query.page() * query.size());
        return new TaxonomyPage(jdbc.query("SELECT * FROM product_taxonomy_category" + where
            + " ORDER BY lower(name),id LIMIT ? OFFSET ?", this::taxonomyRow, args.toArray()), total);
    }

    public TaxonomyCategory createTaxonomy(UUID id, TaxonomyWrite.Fields fields) {
        try {
            jdbc.update("INSERT INTO product_taxonomy_category(id,code,name,description) VALUES (?,?,?,?)", id,
                fields.code(), fields.name(), fields.description());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return taxonomy(id, false);
    }

    public TaxonomyCategory updateTaxonomy(TaxonomyCategory category) {
        try {
            jdbc.update("""
                UPDATE product_taxonomy_category SET code=?,name=?,description=?,active=?,version=version+1,
                    updated_at=greatest(clock_timestamp(),updated_at+interval '1 microsecond') WHERE id=?
                """, category.code(), category.name(), category.description(), category.active(), category.id());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrOriginal(exception);
        }
        return taxonomy(category.id(), false);
    }

    public List<TaxonomyCategory> assignedCategories(UUID product) {
        return jdbc.query("""
            SELECT c.* FROM product_taxonomy_category c JOIN product_taxonomy_assignment a ON a.category_id=c.id
            WHERE a.product_id=? ORDER BY lower(c.name),c.id
            """, this::taxonomyRow, product);
    }

    public void validateCategories(Set<UUID> requested, Set<UUID> current) {
        for (UUID id : requested.stream().sorted().toList()) {
            var rows = jdbc.query("SELECT active FROM product_taxonomy_category WHERE id=? FOR SHARE",
                (rs, row) -> rs.getBoolean("active"), id);
            if (rows.isEmpty() || !rows.getFirst() && !current.contains(id)) throw ProjectProblem.field(400, "categoryIds",
                "invalid_reference", "New taxonomy assignments must reference active existing categories.");
        }
    }

    public void replaceCategories(UUID product, Set<UUID> current, Set<UUID> next) {
        for (UUID id : current) if (!next.contains(id))
            jdbc.update("DELETE FROM product_taxonomy_assignment WHERE product_id=? AND category_id=?", product, id);
        for (UUID id : next) if (!current.contains(id))
            jdbc.update("INSERT INTO product_taxonomy_assignment(product_id,category_id) VALUES (?,?)", product, id);
        jdbc.update("UPDATE product SET version=version+1 WHERE id=?", product);
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
        UUID id = rs.getObject("id", UUID.class);
        return new Product(id, rs.getLong("version"),
            rs.getString("code"), rs.getString("name"), rs.getString("scientific_name"),
            rs.getString("hs_code"), rs.getBoolean("active"), rs.getString("description"), rs.getString("processing_method"),
            rs.getString("storage_requirements"), rs.getString("default_measurement_unit"), assignedCategories(id),
            jdbc.query("SELECT * FROM product_category WHERE product_id=? AND active ORDER BY display_order,code,id", this::gradeRow, id));
    }

    private TaxonomyCategory taxonomyRow(ResultSet rs, int row) throws SQLException {
        return new TaxonomyCategory(rs.getObject("id", UUID.class), rs.getLong("version"), rs.getString("code"),
            rs.getString("name"), rs.getString("description"), rs.getBoolean("active"));
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

    public record TaxonomyPage(List<TaxonomyCategory> items, long total) {}
}
