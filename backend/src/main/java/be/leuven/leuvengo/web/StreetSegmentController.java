package be.leuven.leuvengo.web;

import be.leuven.leuvengo.repository.StreetSegmentRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/segments")
public class StreetSegmentController {

    private final StreetSegmentRepository segments;

    public StreetSegmentController(StreetSegmentRepository segments) {
        this.segments = segments;
    }

    @GetMapping
    public List<Dtos.StreetSegmentDto> list() {
        return segments.findAll().stream().map(Dtos::of).toList();
    }
}
