package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.AlbumDTO;
import local.epul4a.fotoshare.entity.Album;
import org.springframework.stereotype.Component;

@Component
public class AlbumMapper {

    public AlbumDTO toDTO(Album album) {
        if (album == null) return null;
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setName(album.getName());
        dto.setDescription(album.getDescription());
        dto.setOwnerUsername(album.getOwner().getUsername());
        dto.setOwnerId(album.getOwner().getId());
        dto.setCreatedAt(album.getCreatedAt());
        dto.setPhotoCount(album.getPhotos().size());
        return dto;
    }
}

