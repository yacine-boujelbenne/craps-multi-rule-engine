package com.exportdocs.geography.api;

import com.exportdocs.geography.application.GeographyService;
import com.exportdocs.party.api.SetupJson;
import com.exportdocs.party.api.SetupListQuery;
import java.util.List;
import java.util.Set;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GeographyController {
    private final GeographyService service;

    public GeographyController(GeographyService service) {
        this.service = service;
    }

    @PostMapping("/ports")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public GeographyDtos.PortResponse createPort(@jakarta.validation.Valid @RequestBody GeographyDtos.PortWrite body) { return service.createPort(body); }

    @GetMapping("/ports")
    public List<GeographyDtos.PortResponse> ports() { return service.ports(); }

    @GetMapping("/countries")
    public List<GeographyDtos.CountryResponse> countries(@RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.countries();
    }

    @GetMapping("/countries/{countryCode}/cities")
    public GeographyDtos.CityPage cities(@PathVariable String countryCode,
        @RequestParam MultiValueMap<String, String> query) {
        if (!countryCode.matches("[A-Z]{2}")) throw SetupJson.invalid("countryCode",
            "Use an ISO 3166-1 alpha-2 country code.");
        SetupListQuery.rejectUnknown(query, Set.of("q", "page", "size"));
        String q = query.getFirst("q");
        if (q != null && q.length() > 200) throw SetupJson.invalid("q",
            "Prefix must contain 1 to 200 characters.");
        return service.cities(countryCode, SetupListQuery.parse(query, false));
    }
}
