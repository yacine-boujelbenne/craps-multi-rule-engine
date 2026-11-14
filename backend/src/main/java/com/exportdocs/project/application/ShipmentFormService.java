package com.exportdocs.project.application;

import com.exportdocs.project.infrastructure.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class ShipmentFormService {
    public record Field(String key,String label,String type) {}
    public record Section(String key,String title,boolean repeatable,List<Field> fields) {}
    public record Forms(long projectVersion,List<Section> sections,Map<String,JsonNode> data) {}
    private static Field text(String key,String label) { return new Field(key,label,"text"); }
    private static Field country(String key,String label) { return new Field(key,label,"country"); }
    private static Field date(String key,String label) { return new Field(key,label,"date"); }
    private static Field weight(String key,String label) { return new Field(key,label,"decimal"); }
    private static Section section(String key,String title,boolean repeatable,Field... fields) { return new Section(key,title,repeatable,List.of(fields)); }
    private static final List<Section> SECTIONS=List.of(
        section("INVOICE","Invoice",false,text("number","Invoice number"),date("date","Invoice date"),text("paymentTerms","Payment terms"),text("incoterm","Incoterm"),text("notes","Notes")),
        section("PACKING_LIST","Packing list",false,text("number","Packing list number"),date("date","Packing date"),text("packaging","Packaging"),text("storage","Storage conditions"),text("notes","Notes")),
        section("CATCH_CERTIFICATES","Catch certificate",true,text("certificateNumber","Certificate number"),text("vessel","Fishing vessel"),text("registration","Vessel registration"),country("flag","Flag country"),text("lot","Catch lot"),date("catchDate","Catch date"),date("landingDate","Landing date"),text("area","Catch area"),weight("rawKg","Raw catch (kg)")),
        section("PROCESSING","Processing statement",true,text("batch","Batch reference"),text("catchLot","Source catch lot"),date("productionDate","Production date"),date("expiryDate","Expiry date"),text("method","Processing method"),text("notes","Notes")),
        section("TRANSPORT","Transport",true,text("vessel","Shipping vessel"),text("voyage","Voyage"),text("container","Container number"),text("seal","Seal number"),text("lock","Lock number"),date("eta","Estimated arrival"),text("notes","Notes")),
        section("BILL_OF_LADING","Bill of lading",false,text("carrierReference","Carrier BL reference"),date("onBoardDate","On-board date"),text("freightTerms","Freight terms"),text("notes","Notes")),
        section("HEALTH_CERTIFICATE","Health certificate",false,text("reference","Official reference"),text("authority","Issuing authority"),date("issueDate","Issue date"),text("inspection","Inspection details"),text("notes","Notes")),
        section("CERTIFICATE_OF_ORIGIN","Certificate of origin",false,text("reference","Official reference"),country("originCountry","Country of origin"),text("authority","Issuing authority"),date("issueDate","Issue date"),text("notes","Notes")),
        section("SHIPPING_LIST","Shipping list",false,text("reference","Annex reference"),text("notes","Notes (vessels and catch quantities are inherited from catch records)"))
    );
    private final ProjectRepository projects;
    private final ProjectDetailsStore details;
    private final ProjectActorProvider actors;
    private final ShipmentFormStore forms;
    public ShipmentFormService(ProjectRepository projects,ProjectDetailsStore details,ProjectActorProvider actors,ShipmentFormStore forms) {
        this.projects=projects;this.details=details;this.actors=actors;this.forms=forms;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Forms get(UUID id) {
        read(id);
        var project=projects.findById(id).orElseThrow(ProjectProblem::notFound);
        return new Forms(project.getVersion(),SECTIONS,forms.list(id));
    }
    @Transactional
    public Forms save(UUID id,String key,long expectedVersion,JsonNode data) {
        read(id);
        var project=projects.findForUpdate(id).orElseThrow(ProjectProblem::notFound);
        var actor=read(id);
        if (!actor.canWrite() || !details.canEditForm(id,actor,key)) throw new ProjectProblem(403,"Form edit assignment required.",List.of());
        var section=SECTIONS.stream().filter(s->s.key().equals(key)).findFirst().orElseThrow(ProjectProblem::notFound);
        if (project.getVersion()!=expectedVersion) throw ProjectProblem.field(409,"expectedProjectVersion","stale_version","Shipment changed. Reload before saving.");
        validate(section,data);
        if (!Objects.equals(forms.list(id).get(key),data)) {
            details.auditContext(actor.id());
            forms.save(id,key,data);
            project.touch();projects.flush();
        }
        return new Forms(project.getVersion(),SECTIONS,forms.list(id));
    }
    private ProjectActor read(UUID id) {
        var actor=actors.current();
        if (!actor.canRead()) throw new ProjectProblem(403,"Read permission required.",List.of());
        if (!details.visible(id,actor)) throw ProjectProblem.notFound();
        return actor;
    }
    private void validate(Section section,JsonNode data) {
        if(data==null || !data.isArray() || data.size()>(section.repeatable()?500:1)) throw ProjectProblem.field(400,"data","invalid","Invalid number of form records.");
        for(var row:data) {
            if(!row.isObject()) throw ProjectProblem.field(400,"data","invalid","Each record must be an object.");
            row.fields().forEachRemaining(entry->{
                var field=section.fields().stream().filter(f->f.key().equals(entry.getKey())).findFirst()
                    .orElseThrow(()->ProjectProblem.field(400,entry.getKey(),"unknown","Unknown form field."));
                var value=entry.getValue();
                if(!value.isTextual() || value.textValue().length()>2000) throw ProjectProblem.field(400,field.key(),"invalid","Use text up to 2000 characters.");
                String text=value.textValue();
                if(text.isBlank()) return;
                if(field.type().equals("country") && !Set.of(Locale.getISOCountries()).contains(text)) throw ProjectProblem.field(400,field.key(),"invalid","Choose an ISO country.");
                if(field.type().equals("decimal") && !text.matches("[0-9]{1,15}(\\.[0-9]{1,3})?")) throw ProjectProblem.field(400,field.key(),"invalid","Use a nonnegative weight with up to three decimal places.");
                if(field.type().equals("date")) try { if(!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException(); LocalDate.parse(text); }
                    catch(RuntimeException e) { throw ProjectProblem.field(400,field.key(),"invalid","Use a valid ISO date."); }
            });
        }
    }
}
