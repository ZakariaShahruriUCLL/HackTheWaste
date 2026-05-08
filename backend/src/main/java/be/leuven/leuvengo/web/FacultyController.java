package be.leuven.leuvengo.web;

import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faculties")
public class FacultyController {

    private final FacultyRepository faculties;

    public FacultyController(FacultyRepository faculties) {
        this.faculties = faculties;
    }

    @GetMapping
    public List<Dtos.FacultyDto> list() {
        return faculties.findAll().stream().map(Dtos::of).toList();
    }
}
