package com.exportdocs.project.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentFormStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ShipmentFormStore(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    public Map<String, JsonNode> list(UUID project) {
        var result=new LinkedHashMap<String, JsonNode>();
        jdbc.query("SELECT form_key,data::text FROM shipment_form WHERE project_id=? ORDER BY form_key", rs -> {
            try { result.put(rs.getString(1),json.readTree(rs.getString(2))); }
            catch (java.io.IOException e) { throw new IllegalStateException("Invalid saved form",e); }
        },project);
        return result;
    }
    public void save(UUID project,String key,JsonNode data) {
        jdbc.update("""
            INSERT INTO shipment_form(project_id,form_key,data) VALUES (?,?,?::jsonb)
            ON CONFLICT(project_id,form_key) DO UPDATE SET data=excluded.data,updated_at=now()
            """,project,key,data.toString());
    }
}
