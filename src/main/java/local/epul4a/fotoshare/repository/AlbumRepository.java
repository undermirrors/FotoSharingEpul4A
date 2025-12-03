package local.epul4a.fotoshare.repository;

import local.epul4a.fotoshare.entity.Album;
import local.epul4a.fotoshare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByOwner(User owner);
    Page<Album> findByOwner(User owner, Pageable pageable);
}

