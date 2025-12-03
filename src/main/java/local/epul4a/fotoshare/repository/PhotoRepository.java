package local.epul4a.fotoshare.repository;

import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    Page<Photo> findByOwner(User owner, Pageable pageable);
    Page<Photo> findByVisibility(Photo.Visibility visibility, Pageable pageable);

    @Query("SELECT p FROM Photo p WHERE p.visibility = 'PUBLIC' OR p.owner = :user OR EXISTS (SELECT pa FROM Share pa WHERE pa.photo = p AND pa.user = :user)")
    Page<Photo> findAccessiblePhotos(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Photo p INNER JOIN Share pa ON pa.photo = p WHERE pa.user = :user")
    Page<Photo> findSharedWithUser(@Param("user") User user, Pageable pageable);
}

