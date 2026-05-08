package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {
}
