package local.epul4a.fotoshare.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long id;

    @NotEmpty(message = "Le comment ne doit pas être vide")
    private String text;

    private Long photoId;
    private String authorUsername;
    private Long authorId;
    private LocalDateTime createdAt;
}

