package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.entity.Photo;
import org.springframework.stereotype.Component;

@Component
public class PhotoMapper {

    public PhotoDTO toDTO(Photo photo) {
        if (photo == null) return null;
        PhotoDTO dto = new PhotoDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setDescription(photo.getDescription());
        dto.setOriginalFilename(photo.getOriginalFilename());
        dto.setStorageFilename(photo.getStorageFilename());
        dto.setThumbnailFilename(photo.getThumbnailFilename());
        dto.setContentType(photo.getContentType());
        dto.setVisibility(photo.getVisibility().name());
        dto.setOwnerUsername(photo.getOwner().getUsername());
        dto.setOwnerId(photo.getOwner().getId());
        dto.setCreatedAt(photo.getCreatedAt());
        return dto;
    }
}

