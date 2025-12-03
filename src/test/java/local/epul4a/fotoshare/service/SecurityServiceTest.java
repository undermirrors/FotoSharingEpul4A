package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.entity.Share;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.repository.ShareRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import local.epul4a.fotoshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SecurityService
 * Scénarios : User A accède à Photo B privée -> Forbidden / User A accède à Photo B partagée -> Allowed
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du SecurityService")
class SecurityServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityService securityService;

    private User userA;
    private User userB;
    private Photo privatePhotoB;
    private Photo publicPhotoB;
    private Photo sharedPhotoB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setUsername("userA");
        userA.setEmail("userA@example.com");
        userA.setRole(User.Role.USER);

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userB");
        userB.setEmail("userB@example.com");
        userB.setRole(User.Role.USER);

        privatePhotoB = new Photo();
        privatePhotoB.setId(100L);
        privatePhotoB.setTitle("Photo privée de B");
        privatePhotoB.setVisibility(Photo.Visibility.PRIVATE);
        privatePhotoB.setOwner(userB);

        publicPhotoB = new Photo();
        publicPhotoB.setId(101L);
        publicPhotoB.setTitle("Photo publique de B");
        publicPhotoB.setVisibility(Photo.Visibility.PUBLIC);
        publicPhotoB.setOwner(userB);

        sharedPhotoB = new Photo();
        sharedPhotoB.setId(102L);
        sharedPhotoB.setTitle("Photo partagée de B");
        sharedPhotoB.setVisibility(Photo.Visibility.PRIVATE);
        sharedPhotoB.setOwner(userB);
    }

    // ==================== Tests canAccessPhoto ====================

    @Test
    @DisplayName("User A NE PEUT PAS accéder à Photo B privée (non partagée) -> FORBIDDEN")
    void userA_CannotAccess_PrivatePhotoB_NotShared() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));
        when(shareRepository.existsByPhotoAndUser(privatePhotoB, userA)).thenReturn(false);

        boolean canAccess = securityService.canAccessPhoto(authentication, 100L);

        assertFalse(canAccess, "User A ne devrait PAS pouvoir accéder à la photo privée de B");
        verify(photoRepository).findById(100L);
        verify(shareRepository).existsByPhotoAndUser(privatePhotoB, userA);
    }

    @Test
    @DisplayName("User A PEUT accéder à Photo B partagée -> ALLOWED")
    void userA_CanAccess_SharedPhotoB() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(102L)).thenReturn(Optional.of(sharedPhotoB));
        when(shareRepository.existsByPhotoAndUser(sharedPhotoB, userA)).thenReturn(true);

        boolean canAccess = securityService.canAccessPhoto(authentication, 102L);

        assertTrue(canAccess, "User A devrait pouvoir accéder à la photo partagée");
        verify(shareRepository).existsByPhotoAndUser(sharedPhotoB, userA);
    }

    @Test
    @DisplayName("User A PEUT accéder à Photo B publique -> ALLOWED")
    void userA_CanAccess_PublicPhotoB() {
        when(photoRepository.findById(101L)).thenReturn(Optional.of(publicPhotoB));

        boolean canAccess = securityService.canAccessPhoto(authentication, 101L);

        assertTrue(canAccess, "User A devrait pouvoir accéder à la photo publique");
        verify(photoRepository).findById(101L);
    }

    @Test
    @DisplayName("User B (propriétaire) PEUT accéder à sa propre photo privée")
    void owner_CanAccess_OwnPrivatePhoto() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userB");
        when(userRepository.findByUsername("userB")).thenReturn(Optional.of(userB));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canAccess = securityService.canAccessPhoto(authentication, 100L);

        assertTrue(canAccess, "Le propriétaire devrait pouvoir accéder à sa propre photo");
    }

    @Test
    @DisplayName("User non authentifié PEUT accéder à une photo publique")
    void unauthenticated_CanAccess_PublicPhoto() {
        when(photoRepository.findById(101L)).thenReturn(Optional.of(publicPhotoB));

        boolean canAccess = securityService.canAccessPhoto(authentication, 101L);

        assertTrue(canAccess, "Un user non authentifié devrait pouvoir accéder à une photo publique");
    }

    @Test
    @DisplayName("User non authentifié NE PEUT PAS accéder à une photo privée")
    void unauthenticated_CannotAccess_PrivatePhoto() {
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canAccess = securityService.canAccessPhoto(null, 100L);

        assertFalse(canAccess, "Un user non authentifié ne devrait PAS accéder à une photo privée");
    }

    @Test
    @DisplayName("Retourne false si la photo n'existe pas")
    void canAccessPhoto_ReturnsFalse_WhenPhotoNotFound() {
        when(photoRepository.findById(999L)).thenReturn(Optional.empty());

        boolean canAccess = securityService.canAccessPhoto(authentication, 999L);

        assertFalse(canAccess, "Devrait retourner false si la photo n'existe pas");
    }

    // ==================== Tests canEditPhoto ====================

    @Test
    @DisplayName("User A NE PEUT PAS éditer Photo B privée -> FORBIDDEN")
    void userA_CannotEdit_PrivatePhotoB() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));
        when(shareRepository.findByPhotoAndUser(privatePhotoB, userA)).thenReturn(Optional.empty());

        boolean canEdit = securityService.canEditPhoto(authentication, 100L);

        assertFalse(canEdit, "User A ne devrait PAS pouvoir éditer la photo de B");
    }

    @Test
    @DisplayName("User A PEUT éditer Photo B s'il a le niveau ADMIN")
    void userA_CanEdit_PhotoB_WithAdminPermission() {
        Share share = new Share();
        share.setPhoto(sharedPhotoB);
        share.setUser(userA);
        share.setPermissionLevel(Share.PermissionLevel.ADMIN);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(102L)).thenReturn(Optional.of(sharedPhotoB));
        when(shareRepository.findByPhotoAndUser(sharedPhotoB, userA)).thenReturn(Optional.of(share));

        boolean canEdit = securityService.canEditPhoto(authentication, 102L);

        assertTrue(canEdit, "User A avec permission ADMIN devrait pouvoir éditer");
    }

    @Test
    @DisplayName("User B (propriétaire) PEUT éditer sa propre photo")
    void owner_CanEdit_OwnPhoto() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userB");
        when(userRepository.findByUsername("userB")).thenReturn(Optional.of(userB));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canEdit = securityService.canEditPhoto(authentication, 100L);

        assertTrue(canEdit, "Le propriétaire devrait pouvoir éditer sa propre photo");
    }

    @Test
    @DisplayName("User A NE PEUT PAS éditer même avec permission COMMENT")
    void userA_CannotEdit_WithCommentPermission() {
        Share share = new Share();
        share.setPhoto(sharedPhotoB);
        share.setUser(userA);
        share.setPermissionLevel(Share.PermissionLevel.COMMENT);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(102L)).thenReturn(Optional.of(sharedPhotoB));
        when(shareRepository.findByPhotoAndUser(sharedPhotoB, userA)).thenReturn(Optional.of(share));

        boolean canEdit = securityService.canEditPhoto(authentication, 102L);

        assertFalse(canEdit, "User A avec permission COMMENT ne devrait PAS pouvoir éditer");
    }

    // ==================== Tests canDeletePhoto ====================

    @Test
    @DisplayName("User A NE PEUT PAS supprimer Photo B -> FORBIDDEN")
    void userA_CannotDelete_PhotoB() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canDelete = securityService.canDeletePhoto(authentication, 100L);

        assertFalse(canDelete, "User A ne devrait PAS pouvoir supprimer la photo de B");
    }

    @Test
    @DisplayName("User B (propriétaire) PEUT supprimer sa propre photo")
    void owner_CanDelete_OwnPhoto() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userB");
        when(userRepository.findByUsername("userB")).thenReturn(Optional.of(userB));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canDelete = securityService.canDeletePhoto(authentication, 100L);

        assertTrue(canDelete, "Le propriétaire devrait pouvoir supprimer sa propre photo");
    }

    @Test
    @DisplayName("ADMIN peut supprimer n'importe quelle photo")
    void admin_CanDelete_AnyPhoto() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(User.Role.ADMIN);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));

        boolean canDelete = securityService.canDeletePhoto(authentication, 100L);

        assertTrue(canDelete, "Un ADMIN devrait pouvoir supprimer n'importe quelle photo");
    }

    // ==================== Tests canCommentPhoto ====================

    @Test
    @DisplayName("User A NE PEUT PAS commenter Photo B publique")
    void userA_CanComment_PublicPhotoB() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(101L)).thenReturn(Optional.of(publicPhotoB));
        when(shareRepository.findByPhotoAndUser(publicPhotoB, userA)).thenReturn(Optional.empty());

        boolean canComment = securityService.canCommentPhoto(authentication, 101L);

        assertFalse(canComment, "User A ne devrait PAS pouvoir commenter une photo publique");
    }

    @Test
    @DisplayName("User A PEUT commenter Photo B partagée avec permission COMMENT")
    void userA_CanComment_SharedPhotoB_WithCommentPermission() {
        Share share = new Share();
        share.setPhoto(sharedPhotoB);
        share.setUser(userA);
        share.setPermissionLevel(Share.PermissionLevel.COMMENT);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(102L)).thenReturn(Optional.of(sharedPhotoB));
        when(shareRepository.findByPhotoAndUser(sharedPhotoB, userA)).thenReturn(Optional.of(share));

        boolean canComment = securityService.canCommentPhoto(authentication, 102L);

        assertTrue(canComment, "User A avec permission COMMENT devrait pouvoir commenter");
    }

    @Test
    @DisplayName("User A NE PEUT PAS commenter Photo B privée non partagée")
    void userA_CannotComment_PrivatePhotoB_NotShared() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("userA");
        when(userRepository.findByUsername("userA")).thenReturn(Optional.of(userA));
        when(photoRepository.findById(100L)).thenReturn(Optional.of(privatePhotoB));
        when(shareRepository.findByPhotoAndUser(privatePhotoB, userA)).thenReturn(Optional.empty());

        boolean canComment = securityService.canCommentPhoto(authentication, 100L);

        assertFalse(canComment, "User A ne devrait PAS pouvoir commenter une photo privée non partagée");
    }

    // ==================== Tests isOwner ====================

    @Test
    @DisplayName("isOwner retourne true pour le propriétaire")
    void isOwner_ReturnsTrue_ForOwner() {
        boolean isOwner = securityService.isOwner(userB, privatePhotoB);

        assertTrue(isOwner, "userB est bien le propriétaire de privatePhotoB");
    }

    @Test
    @DisplayName("isOwner retourne false pour un non-propriétaire")
    void isOwner_ReturnsFalse_ForNonOwner() {
        boolean isOwner = securityService.isOwner(userA, privatePhotoB);

        assertFalse(isOwner, "userA n'est pas le propriétaire de privatePhotoB");
    }

    // ==================== Tests getPermissionLevel ====================

    @Test
    @DisplayName("getPermissionLevel retourne le niveau correct")
    void getPermissionLevel_ReturnsCorrectLevel() {
        Share share = new Share();
        share.setPhoto(sharedPhotoB);
        share.setUser(userA);
        share.setPermissionLevel(Share.PermissionLevel.ADMIN);

        when(shareRepository.findByPhotoAndUser(sharedPhotoB, userA)).thenReturn(Optional.of(share));

        Share.PermissionLevel level = securityService.getPermissionLevel(userA, sharedPhotoB);

        assertEquals(Share.PermissionLevel.ADMIN, level, "Le niveau de permission devrait être ADMIN");
    }

    @Test
    @DisplayName("getPermissionLevel retourne null si pas de share")
    void getPermissionLevel_ReturnsNull_WhenNoShare() {
        when(shareRepository.findByPhotoAndUser(privatePhotoB, userA)).thenReturn(Optional.empty());

        Share.PermissionLevel level = securityService.getPermissionLevel(userA, privatePhotoB);

        assertNull(level, "Le niveau de permission devrait être null s'il n'y a pas de share");
    }
}

