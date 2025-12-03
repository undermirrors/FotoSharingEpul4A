package local.epul4a.fotoshare.repository;

import local.epul4a.fotoshare.entity.Share;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {
    List<Share> findByPhoto(Photo photo);
    List<Share> findByUser(User user);
    Optional<Share> findByPhotoAndUser(Photo photo, User user);
    boolean existsByPhotoAndUser(Photo photo, User user);
}

