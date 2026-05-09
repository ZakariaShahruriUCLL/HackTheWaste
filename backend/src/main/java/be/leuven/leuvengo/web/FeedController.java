package be.leuven.leuvengo.web;

import be.leuven.leuvengo.repository.TrashPhotoRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final TrashPhotoRepository photos;

    public FeedController(TrashPhotoRepository photos) {
        this.photos = photos;
    }

    /**
     * Paginated photo feed, newest first.
     * Optional ?clan=APO filter returns only that clan's photos.
     */
    @GetMapping
    public Page<Dtos.TrashPhotoDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String clan) {

        PageRequest pr = PageRequest.of(page, Math.min(size, 48));
        Page<Dtos.TrashPhotoDto> result = clan != null && !clan.isBlank()
                ? photos.findAllByFacultyShortCodeOrderByReportedAtDesc(clan, pr).map(Dtos::of)
                : photos.findAllByOrderByReportedAtDesc(pr).map(Dtos::of);
        return result;
    }
}
