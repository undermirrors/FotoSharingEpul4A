package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.entity.Album;
import local.epul4a.fotoshare.entity.Share;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.repository.AlbumRepository;
import local.epul4a.fotoshare.repository.ShareRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import local.epul4a.fotoshare.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("securityService")
@Transactional(readOnly = true)
public class SecurityService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final ShareRepository shareRepository;
    private final AlbumRepository albumRepository;

    public SecurityService(PhotoRepository photoRepository, UserRepository userRepository, ShareRepository shareRepository, AlbumRepository albumRepository) {
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.shareRepository = shareRepository;
        this.albumRepository = albumRepository;
    }

    public boolean canAccessPhoto(Authentication authentication, Long photoId) {
        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) return false;
        if (photo.getVisibility() == Photo.Visibility.PUBLIC) return true;

        User user = getUserFromAuth(authentication);
        if (user == null) return false;
        if (user.getRole() == User.Role.ADMIN) return true;

        return isOwner(user, photo) || shareRepository.existsByPhotoAndUser(photo, user);
    }

    public boolean canEditPhoto(Authentication authentication, Long photoId) {
        User user = getUserFromAuth(authentication);
        if (user == null) return false;

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) return false;

        if (isOwner(user, photo)) return true;

        Share.PermissionLevel level = getPermissionLevel(user, photo);
        return level == Share.PermissionLevel.ADMIN;
    }

    public boolean canDeletePhoto(Authentication authentication, Long photoId) {
        User user = getUserFromAuth(authentication);
        if (user == null) return false;

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) return false;

        if (user.getRole() == User.Role.ADMIN) return true;

        if (isOwner(user, photo)) return true;

        if (user.getRole() == User.Role.MODERATOR) {
            return (photo.getVisibility() == Photo.Visibility.PUBLIC) || shareRepository.existsByPhotoAndUser(photo, user);
        }

        return false;
    }

    public boolean canCommentPhoto(Authentication authentication, Long photoId) {
        User user = getUserFromAuth(authentication);
        if (user == null) return false;

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null) return false;

        if (user.getRole() == User.Role.ADMIN || isOwner(user, photo)) {
            return true;
        }

        Share.PermissionLevel level = getPermissionLevel(user, photo);
        return level == Share.PermissionLevel.COMMENT || level == Share.PermissionLevel.ADMIN;
    }

    public boolean isOwner(User user, Photo photo) {
        return photo.getOwner().getId().equals(user.getId());
    }

    public boolean isOwner(User user, Album album) {
        return album.getOwner().getId().equals(user.getId());
    }

    public boolean canAccessAlbum(Authentication authentication, Long albumId) {
        User user = getUserFromAuth(authentication);
        if (user == null) return false;

        Album album = albumRepository.findById(albumId).orElse(null);
        if (album == null) return false;

        if (user.getRole() == User.Role.ADMIN) return true;

        return isOwner(user, album);
    }

    public boolean canEditAlbum(Authentication authentication, Long albumId) {
        User user = getUserFromAuth(authentication);
        if (user == null) return false;

        Album album = albumRepository.findById(albumId).orElse(null);
        if (album == null) return false;

        return isOwner(user, album) || user.getRole() == User.Role.ADMIN;
    }

    public Share.PermissionLevel getPermissionLevel(User user, Photo photo) {
        return shareRepository.findByPhotoAndUser(photo, user).map(Share::getPermissionLevel).orElse(null);
    }

    private User getUserFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}

