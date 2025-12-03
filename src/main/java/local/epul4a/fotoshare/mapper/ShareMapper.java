package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.ShareDTO;
import local.epul4a.fotoshare.entity.Share;
import org.springframework.stereotype.Component;

@Component
public class ShareMapper {

    public ShareDTO toDTO(Share share) {
        if (share == null) return null;
        ShareDTO dto = new ShareDTO();
        dto.setId(share.getId());
        dto.setPhotoId(share.getPhoto().getId());
        dto.setPhotoTitle(share.getPhoto().getTitle());
        dto.setUserId(share.getUser().getId());
        dto.setUsername(share.getUser().getUsername());
        dto.setPermissionLevel(share.getPermissionLevel().name());
        dto.setCreatedAt(share.getCreatedAt());
        return dto;
    }
}

