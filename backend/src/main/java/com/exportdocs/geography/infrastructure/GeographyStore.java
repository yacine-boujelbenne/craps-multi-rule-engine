package com.exportdocs.geography.infrastructure;

import com.exportdocs.geography.domain.City;
import com.exportdocs.geography.domain.Country;
import com.exportdocs.party.api.SetupListQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GeographyStore {
    private final JdbcTemplate jdbc;

    public GeographyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<com.exportdocs.geography.api.GeographyDtos.PortResponse> ports() {
        return jdbc.query("SELECT id,name,country_code,un_locode,active FROM port WHERE active ORDER BY lower(name),id",
            (rs, row) -> new com.exportdocs.geography.api.GeographyDtos.PortResponse(rs.getObject("id", UUID.class),
                rs.getString("name"), rs.getString("country_code"), rs.getString("un_locode"), rs.getBoolean("active")));
    }

    public com.exportdocs.geography.api.GeographyDtos.PortResponse createPort(com.exportdocs.geography.api.GeographyDtos.PortWrite input) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO port(id,name,country_code) VALUES (?,?,?)",id,input.name().trim(),input.countryCode());
        return new com.exportdocs.geography.api.GeographyDtos.PortResponse(id,input.name().trim(),input.countryCode(),null,true);
    }

    public List<Country> countries() {
        return jdbc.query("SELECT code,name FROM country WHERE active ORDER BY lower(name),code",
            (rs, row) -> new Country(rs.getString("code"), rs.getString("name")));
    }

    public boolean activeCountry(String code) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM country WHERE code=? AND active)", Boolean.class, code));
    }

    public CityPage cities(String countryCode, SetupListQuery query) {
        String where = " WHERE country_code=? AND active";
        var args = new ArrayList<Object>();
        args.add(countryCode);
        if (query.prefix() != null) {
            where += " AND lower(name) LIKE ? ESCAPE '!'";
            args.add(query.prefix());
        }
        long total = jdbc.queryForObject("SELECT count(*) FROM city" + where, Long.class, args.toArray());
        args.add(query.size());
        args.add((long)query.page() * query.size());
        var cities = jdbc.query("SELECT id,country_code,name,active FROM city" + where
            + " ORDER BY lower(name),id LIMIT ? OFFSET ?",
            (rs, row) -> new City(rs.getObject("id", UUID.class), rs.getString("country_code"),
                rs.getString("name"), rs.getBoolean("active")), args.toArray());
        return new CityPage(cities, total);
    }

    public record CityPage(List<City> items, long total) {}
}
