package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.dto.ShareDTO;
import local.epul4a.fotoshare.entity.Share;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.mapper.ShareMapper;
import local.epul4a.fotoshare.repository.ShareRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import local.epul4a.fotoshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShareService {

    private final ShareRepository shareRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final ShareMapper shareMapper;

    public ShareService(ShareRepository shareRepository, PhotoRepository photoRepository,
                          UserRepository userRepository, SecurityService securityService, ShareMapper shareMapper) {
        this.shareRepository = shareRepository;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.securityService = securityService;
        this.shareMapper = shareMapper;
    }

    public void sharePhoto(Long photoId, Long userId, Share.PermissionLevel permissionLevel, User owner) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));

        if (!securityService.isOwner(owner, photo)) {
            throw new RuntimeException("Accès refusé");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        if (shareRepository.existsByPhotoAndUser(photo, targetUser)) {
            throw new RuntimeException("Cette photo est déjà partagée avec cet user");
        }

        Share share = new Share();
        share.setPhoto(photo);
        share.setUser(targetUser);
        share.setPermissionLevel(permissionLevel);
        shareRepository.save(share);
    }

    public void updatePermission(Long partageId, Share.PermissionLevel permissionLevel, User owner) {
        Share share = shareRepository.findById(partageId)
                .orElseThrow(() -> new RuntimeException("Share introuvable"));

        if (!securityService.isOwner(owner, share.getPhoto())) {
            throw new RuntimeException("Accès refusé");
        }

        share.setPermissionLevel(permissionLevel);
        shareRepository.save(share);
    }

    public void revokeAccess(Long partageId, User owner) {
        Share share = shareRepository.findById(partageId)
                .orElseThrow(() -> new RuntimeException("Share introuvable"));

        if (!securityService.isOwner(owner, share.getPhoto())) {
            throw new RuntimeException("Accès refusé");
        }
        shareRepository.delete(share);
    }

    @Transactional(readOnly = true)
    public List<ShareDTO> getPhotoShares(Long photoId, User owner) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));

        if (!securityService.isOwner(owner, photo)) {
            throw new RuntimeException("Accès refusé");
        }

        return shareRepository.findByPhoto(photo).stream()
                .map(shareMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShareDTO> getUserShares(User user) {
        return shareRepository.findByUser(user).stream()
                .map(shareMapper::toDTO)
                .collect(Collectors.toList());
    }
}

