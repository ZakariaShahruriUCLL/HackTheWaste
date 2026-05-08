package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.Faculty;
import be.leuven.leuvengo.repository.FacultyRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final FacultyRepository faculties;

    public LeaderboardController(FacultyRepository faculties) {
        this.faculties = faculties;
    }

    @GetMapping
    public List<Dtos.LeaderboardEntry> get() {
        List<Faculty> sorted = faculties.findAll().stream()
                .sorted(Comparator.comparing(
                        (Faculty f) -> f.getPoints() == null ? 0 : f.getPoints()).reversed())
                .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    Faculty f = sorted.get(i);
                    return new Dtos.LeaderboardEntry(
                            f.getId(), f.getName(), f.getShortCode(), f.getColor(),
                            f.getEmoji(),
                            f.getPoints() == null ? 0 : f.getPoints(),
                            i + 1);
                })
                .toList();
    }
}
