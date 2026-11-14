package com.exportdocs.party.infrastructure;

import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.domain.Organization;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.domain.OrganizationAddress;
import com.exportdocs.project.application.ProjectProblem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationStore {
    private final JdbcTemplate jdbc;

    public OrganizationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Organization find(UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM organization WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::row, id);
        if (rows.isEmpty()) throw ProjectProblem.notFound();
        return rows.getFirst();
    }

    public Organization create(UUID id, com.exportdocs.party.application.OrganizationWrite.Fields fields) {
        jdbc.update("""
            INSERT INTO organization(id,legal_name,country_code,address_text,registration_number,trade_name,tax_identifier,email,phone,active)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """, id, fields.legalName(), fields.countryCode(), fields.addressText(), fields.registrationNumber(),
            fields.tradeName(), fields.taxIdentifier(), fields.email(), fields.phone(), fields.active());
        for (var role : fields.roles())jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,?)", id, role.name());
        return find(id, false);
    }

    public Organization update(Organization next, Organization current) {
        var removed = new HashSet<>(current.roles());
        removed.removeAll(next.roles());
        for (var role : removed) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM project_party WHERE organization_id=? AND role=?)", Boolean.class, next.id(),
                role.name()))) throw roleInUse();
        }
        jdbc.update("""
                UPDATE organization SET legal_name=?,country_code=?,address_text=?,registration_number=?,
                trade_name=?,tax_identifier=?,email=?,phone=?,active=?,
                version=version+1,updated_at=greatest(clock_timestamp(),updated_at+interval '1 microsecond') WHERE id=?
                """, next.legalName(), next.countryCode(), next.addressText(), next.registrationNumber(),
            next.tradeName(), next.taxIdentifier(), next.email(), next.phone(), next.active(), next.id());
        try {
            for (var role : removed)jdbc.update("DELETE FROM organization_role WHERE organization_id=? AND role=?", next.id(), role.name());
        } catch (DataIntegrityViolationException exception) {
            if (sqlState(exception, "23503")) throw roleInUse();
            throw exception;
        }
        var added = new HashSet<>(next.roles());
        added.removeAll(current.roles());
        for (var role : added)jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,?)", next.id(), role.name());
        return find(next.id(), false);
    }

    public Page list(SetupListQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!query.includeInactive())where.append(" AND o.active");
        if (query.prefix() != null) {
            where.append(" AND lower(o.legal_name) LIKE ? ESCAPE '!'");
            args.add(query.prefix());
        }
        if (query.role() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM organization_role r WHERE r.organization_id=o.id AND r.role=?)");
            args.add(query.role());
        }
        long total = jdbc.queryForObject("SELECT count(*) FROM organization o" + where, Long.class, args.toArray());
        args.add(query.size());
        args.add((long)query.page()*query.size());
        var rows = jdbc.query("SELECT o.* FROM organization o" + where + " ORDER BY o.legal_name,o.id LIMIT ? OFFSET ?", this::row, args.toArray());
        return new Page(rows, total);
    }

    public List<OrganizationAddress> addresses(UUID organization) {
        return jdbc.query("SELECT * FROM organization_address WHERE organization_id=? ORDER BY address_kind,created_at,id",
            this::addressRow, organization);
    }

    public OrganizationAddress address(UUID organization, UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM organization_address WHERE organization_id=? AND id=?"
            + (lock ? " FOR UPDATE" : ""), this::addressRow, organization, id);
        if (rows.isEmpty()) throw ProjectProblem.field(404, "addressId", "not_found", "Address not found.");
        return rows.getFirst();
    }

    public OrganizationAddress createAddress(OrganizationAddress address) {
        jdbc.update("""
            INSERT INTO organization_address(id,organization_id,address_kind,country_code,city_id,manual_city,
                address_line1,address_line2,postal_code)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, address.id(), address.organizationId(), address.kind().name(), address.countryCode(), address.cityId(),
            address.manualCity(), address.addressLine1(), address.addressLine2(), address.postalCode());
        return address(address.organizationId(), address.id(), false);
    }

    public OrganizationAddress updateAddress(OrganizationAddress address) {
        jdbc.update("""
            UPDATE organization_address SET address_kind=?,country_code=?,city_id=?,manual_city=?,address_line1=?,
                address_line2=?,postal_code=?,active=?,version=version+1,
                updated_at=greatest(clock_timestamp(),updated_at+interval '1 microsecond')
            WHERE organization_id=? AND id=?
            """, address.kind().name(), address.countryCode(), address.cityId(), address.manualCity(),
            address.addressLine1(), address.addressLine2(), address.postalCode(), address.active(), address.organizationId(), address.id());
        return address(address.organizationId(), address.id(), false);
    }

    public void validateAddressLocation(OrganizationAddress address) {
        var countries = jdbc.query("SELECT active FROM country WHERE code=? FOR SHARE",
            (rs, row) -> rs.getBoolean("active"), address.countryCode());
        if (countries.isEmpty() || !countries.getFirst()) throw ProjectProblem.field(400, "countryCode", "invalid_reference",
            "Country must exist and be active.");
        if (address.cityId() != null) {
            var cities = jdbc.query("SELECT country_code,active FROM city WHERE id=? FOR SHARE",
                (rs, row) -> rs.getBoolean("active") && address.countryCode().equals(rs.getString("country_code")),
                address.cityId());
            if (cities.isEmpty() || !cities.getFirst()) throw ProjectProblem.field(400, "cityId", "invalid_reference",
                "City must exist, be active and belong to the selected country.");
        }
    }

    private Organization row(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        Set<OrganizationRole> roles = new HashSet<>(jdbc.query("SELECT role FROM organization_role WHERE organization_id=? ORDER BY role", (role,
            index) -> OrganizationRole.valueOf(role.getString(1)), id));
        return new Organization(id, rs.getLong("version"), rs.getString("legal_name"),
            rs.getString("country_code"), rs.getString("address_text"), rs.getString("registration_number"),
            rs.getString("trade_name"), rs.getString("tax_identifier"), rs.getString("email"), rs.getString("phone"),
            Set.copyOf(roles), rs.getBoolean("active"), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(), addresses(id));
    }

    private OrganizationAddress addressRow(ResultSet rs, int row) throws SQLException {
        return new OrganizationAddress(rs.getObject("id", UUID.class), rs.getObject("organization_id", UUID.class),
            OrganizationAddress.Kind.valueOf(rs.getString("address_kind")), rs.getString("country_code"),
            rs.getObject("city_id", UUID.class), rs.getString("manual_city"), rs.getString("address_line1"),
            rs.getString("address_line2"), rs.getString("postal_code"), rs.getBoolean("active"), rs.getLong("version"));
    }

    public static boolean sqlState(Throwable exception, String state) {
        for (Throwable cause = exception;cause != null;cause = cause.getCause())if (cause instanceof SQLException sql && state.equals(sql.getSQLState())) return true;
        return false;
    }

    private static ProjectProblem roleInUse() {
        return ProjectProblem.field(409, "roles", "role_in_use", "A removed role is still linked to a shipment.");
    }

    public record Page(List<Organization> items, long total) {
    }
}
