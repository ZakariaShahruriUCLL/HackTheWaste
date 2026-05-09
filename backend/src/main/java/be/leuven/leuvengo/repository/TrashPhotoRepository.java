package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.TrashPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TrashPhotoRepository extends JpaRepository<TrashPhoto, Long> {

    Page<TrashPhoto> findAllByOrderByReportedAtDesc(Pageable pageable);

    Page<TrashPhoto> findAllByFacultyShortCodeOrderByReportedAtDesc(
            String facultyShortCode, Pageable pageable);
}
