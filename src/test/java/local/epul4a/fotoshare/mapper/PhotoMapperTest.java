package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du mapper Photo")
class PhotoMapperTest {

    private PhotoMapper photoMapper;
    private User testOwner;
    private Photo testPhoto;

    @BeforeEach
    void setUp() {
        photoMapper = new PhotoMapper();

        testOwner = new User();
        testOwner.setId(1L);
        testOwner.setUsername("testuser");
        testOwner.setEmail("test@example.com");
        testOwner.setPasswordHash("hashedpassword");
        testOwner.setRole(User.Role.USER);
        testOwner.setEnabled(true);

        testPhoto = new Photo();
        testPhoto.setId(100L);
        testPhoto.setTitle("Ma Photo Test");
        testPhoto.setDescription("Description de test");
        testPhoto.setOriginalFilename("photo.jpg");
        testPhoto.setStorageFilename("uuid-photo.jpg");
        testPhoto.setThumbnailFilename("thumb_uuid-photo.jpg");
        testPhoto.setContentType("image/jpeg");
        testPhoto.setVisibility(Photo.Visibility.PRIVATE);
        testPhoto.setOwner(testOwner);
        testPhoto.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
    }

    @Test
    @DisplayName("Devrait convertir Photo vers PhotoDTO correctement")
    void shouldConvertPhotoToDTO() {
        PhotoDTO dto = photoMapper.toDTO(testPhoto);
        assertNotNull(dto, "Le DTO ne doit pas être null");
        assertEquals(testPhoto.getId(), dto.getId(), "L'ID doit correspondre");
        assertEquals(testPhoto.getTitle(), dto.getTitle(), "Le titre doit correspondre");
        assertEquals(testPhoto.getDescription(), dto.getDescription(), "La description doit correspondre");
        assertEquals(testPhoto.getOriginalFilename(), dto.getOriginalFilename(), "Le nom de fichier original doit correspondre");
        assertEquals(testPhoto.getStorageFilename(), dto.getStorageFilename(), "Le nom de fichier de stockage doit correspondre");
        assertEquals(testPhoto.getVisibility().name(), dto.getVisibility(), "La visibilité doit correspondre");
        assertEquals(testPhoto.getOwner().getUsername(), dto.getOwnerUsername(), "Le nom d'user du propriétaire doit correspondre");
        assertEquals(testPhoto.getOwner().getId(), dto.getOwnerId(), "L'ID du propriétaire doit correspondre");
        assertEquals(testPhoto.getCreatedAt(), dto.getCreatedAt(), "La date de création doit correspondre");
    }

    @Test
    @DisplayName("Devrait convertir Photo PUBLIC vers DTO avec la bonne visibilité")
    void shouldConvertPublicPhotoToDTO() {
        testPhoto.setVisibility(Photo.Visibility.PUBLIC);

        PhotoDTO dto = photoMapper.toDTO(testPhoto);

        assertEquals("PUBLIC", dto.getVisibility(), "La visibilité doit être PUBLIC");
    }

    @Test
    @DisplayName("Devrait gérer une photo sans description")
    void shouldHandlePhotoWithoutDescription() {
        testPhoto.setDescription(null);

        PhotoDTO dto = photoMapper.toDTO(testPhoto);

        assertNull(dto.getDescription(), "La description doit être null");
        assertNotNull(dto.getTitle(), "Le titre ne doit pas être null");
    }

    @Test
    @DisplayName("Devrait préserver les informations du propriétaire")
    void shouldPreserveOwnerInformation() {
        testOwner.setUsername("proprietaire123");
        testOwner.setId(999L);

        PhotoDTO dto = photoMapper.toDTO(testPhoto);

        assertEquals("proprietaire123", dto.getOwnerUsername(), "Le nom d'user du propriétaire doit être correct");
        assertEquals(999L, dto.getOwnerId(), "L'ID du propriétaire doit être correct");
    }

    @Test
    @DisplayName("Devrait convertir plusieurs photos avec des propriétaires différents")
    void shouldConvertMultiplePhotosWithDifferentOwners() {
        User anotherOwner = new User();
        anotherOwner.setId(2L);
        anotherOwner.setUsername("autreuser");
        anotherOwner.setEmail("autre@example.com");

        Photo anotherPhoto = new Photo();
        anotherPhoto.setId(200L);
        anotherPhoto.setTitle("Autre Photo");
        anotherPhoto.setDescription("Autre description");
        anotherPhoto.setOriginalFilename("autre.png");
        anotherPhoto.setStorageFilename("uuid-autre.png");
        anotherPhoto.setThumbnailFilename("thumb_uuid-autre.png");
        anotherPhoto.setContentType("image/png");
        anotherPhoto.setVisibility(Photo.Visibility.PUBLIC);
        anotherPhoto.setOwner(anotherOwner);
        anotherPhoto.setCreatedAt(LocalDateTime.of(2025, 2, 1, 14, 30));

        PhotoDTO dto1 = photoMapper.toDTO(testPhoto);
        PhotoDTO dto2 = photoMapper.toDTO(anotherPhoto);

        assertEquals("testuser", dto1.getOwnerUsername(), "Le premier propriétaire doit être correct");
        assertEquals("autreuser", dto2.getOwnerUsername(), "Le second propriétaire doit être correct");
        assertNotEquals(dto1.getId(), dto2.getId(), "Les IDs doivent être différents");
    }
}

