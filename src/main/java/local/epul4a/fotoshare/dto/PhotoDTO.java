package local.epul4a.fotoshare.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDTO {
    private Long id;
    private String title;
    private String description;
    private String originalFilename;
    private String storageFilename;
    private String thumbnailFilename;
    private String contentType;
    private String visibility;
    private String ownerUsername;
    private Long ownerId;
    private LocalDateTime createdAt;
}

