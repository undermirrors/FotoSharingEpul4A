package local.epul4a.fotoshare.repository;

import local.epul4a.fotoshare.entity.Comment;
import local.epul4a.fotoshare.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPhoto(Photo photo);
    Page<Comment> findByPhoto(Photo photo, Pageable pageable);
}

