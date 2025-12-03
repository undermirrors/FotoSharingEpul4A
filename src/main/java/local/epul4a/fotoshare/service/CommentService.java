package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.dto.CommentDTO;
import local.epul4a.fotoshare.entity.Comment;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.mapper.CommentMapper;
import local.epul4a.fotoshare.repository.CommentRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PhotoRepository photoRepository;
    private final CommentMapper commentMapper;

    public CommentService(CommentRepository commentRepository, PhotoRepository photoRepository, CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.photoRepository = photoRepository;
        this.commentMapper = commentMapper;
    }

    public void addComment(Long photoId, String text, User author) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));

        Comment comment = new Comment();
        comment.setPhoto(photo);
        comment.setText(text);
        comment.setAuthor(author);
        commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment introuvable"));

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isAdminOrModerator = user.getRole() == User.Role.ADMIN
                                   || user.getRole() == User.Role.MODERATOR;

        if (!isAuthor && !isAdminOrModerator) {
            throw new RuntimeException("Accès refusé");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentDTO> getPhotoComments(Long photoId, Pageable pageable) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        return commentRepository.findByPhoto(photo, pageable).map(commentMapper::toDTO);
    }
}
