package be.leuven.leuvengo.web;

import be.leuven.leuvengo.repository.RewardItemRepository;
import be.leuven.leuvengo.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardItemRepository rewards;

    public RewardController(RewardItemRepository rewards) {
        this.rewards = rewards;
    }

    @GetMapping
    public List<Dtos.RewardItemDto> list() {
        return rewards.findAll().stream().map(Dtos::of).toList();
    }
}
