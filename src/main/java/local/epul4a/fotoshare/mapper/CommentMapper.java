package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.CommentDTO;
import local.epul4a.fotoshare.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentDTO toDTO(Comment comment) {
        if (comment == null) return null;
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setPhotoId(comment.getPhoto().getId());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setAuthorId(comment.getAuthor().getId());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}

