package local.epul4a.fotoshare.integration;

import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.dto.PhotoUploadDTO;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.repository.PhotoRepository;
import local.epul4a.fotoshare.repository.UserRepository;
import local.epul4a.fotoshare.service.PhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration : Flux complet d'upload de photo
 * Vérifie : Envoi fichier -> Vérification présence sur disque -> Vérification entrée en BDD
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration - Upload de Photo")
class PhotoUploadIntegrationTest {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${fotoshare.upload.directory:uploads}")
    private String uploadDirectory;

    private User testUser;
    private Path uploadPath;

    @BeforeEach
    void setUp() throws IOException {
        // Créer un user de test
        testUser = new User();
        testUser.setUsername("testupload");
        testUser.setEmail("testupload@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setRole(User.Role.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Préparer le répertoire d'upload
        uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @AfterEach
    void cleanup() throws IOException {
        // Nettoyer les fichiers uploadés pendant les tests
        if (Files.exists(uploadPath)) {
            try (var stream = Files.list(uploadPath)) {
                stream.filter(path -> path.getFileName().toString().startsWith("test-"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Ignorer les erreurs de nettoyage
                            }
                        });
            }
        }
    }

    @Test
    @DisplayName("Test flux complet : Upload fichier -> Vérification disque -> Vérification BDD")
    void testCompleteUploadFlow() throws IOException {
        // ===== ÉTAPE 1 : Préparer le fichier à uploader =====
        byte[] imageContent = createTestImageContent();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-photo.jpg",
                "image/jpeg",
                imageContent
        );

        PhotoUploadDTO uploadDTO = new PhotoUploadDTO();
        uploadDTO.setTitle("Photo de test intégration");
        uploadDTO.setDescription("Description de la photo de test");
        uploadDTO.setVisibility("PRIVATE");
        uploadDTO.setFile(file);

        // ===== ÉTAPE 2 : Upload via le service =====
        PhotoDTO uploadedPhoto = photoService.uploadPhoto(uploadDTO, testUser);

        // Vérifications immédiates
        assertNotNull(uploadedPhoto, "La photo uploadée ne doit pas être null");
        assertNotNull(uploadedPhoto.getId(), "L'ID de la photo doit être généré");
        assertEquals("Photo de test intégration", uploadedPhoto.getTitle());
        assertEquals("Description de la photo de test", uploadedPhoto.getDescription());
        assertEquals("PRIVATE", uploadedPhoto.getVisibility());
        assertEquals(testUser.getUsername(), uploadedPhoto.getOwnerUsername());

        // ===== ÉTAPE 3 : Vérification présence sur le disque =====
        String storageFilename = uploadedPhoto.getStorageFilename();
        assertNotNull(storageFilename, "Le nom de fichier de stockage doit être défini");
        
        Path uploadedFilePath = uploadPath.resolve(storageFilename);
        assertTrue(Files.exists(uploadedFilePath), 
                "Le fichier doit exister sur le disque : " + uploadedFilePath);
        
        byte[] savedContent = Files.readAllBytes(uploadedFilePath);
        assertArrayEquals(imageContent, savedContent, 
                "Le contenu du fichier sur disque doit correspondre au contenu uploadé");

        // ===== ÉTAPE 4 : Vérification entrée en BDD =====
        Photo photoInDb = photoRepository.findById(uploadedPhoto.getId()).orElse(null);
        assertNotNull(photoInDb, "La photo doit exister en base de données");
        assertEquals("Photo de test intégration", photoInDb.getTitle());
        assertEquals("Description de la photo de test", photoInDb.getDescription());
        assertEquals(Photo.Visibility.PRIVATE, photoInDb.getVisibility());
        assertEquals(testUser.getId(), photoInDb.getOwner().getId());
        assertEquals("test-photo.jpg", photoInDb.getOriginalFilename());
        assertEquals(storageFilename, photoInDb.getStorageFilename());
        assertEquals("image/jpeg", photoInDb.getContentType());
        assertNotNull(photoInDb.getCreatedAt(), "La date de création doit être définie");

        // Nettoyage explicite du fichier de test
        Files.deleteIfExists(uploadedFilePath);
    }

    @Test
    @DisplayName("Test upload avec différents types de fichiers")
    void testUploadDifferentFileTypes() throws IOException {
        // Test avec PNG
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "test-photo.png",
                "image/png",
                createTestImageContent()
        );

        PhotoUploadDTO pngUploadDTO = new PhotoUploadDTO();
        pngUploadDTO.setTitle("Photo PNG");
        pngUploadDTO.setDescription("Test PNG");
        pngUploadDTO.setVisibility("PUBLIC");
        pngUploadDTO.setFile(pngFile);

        PhotoDTO pngPhoto = photoService.uploadPhoto(pngUploadDTO, testUser);
        
        assertNotNull(pngPhoto);
        assertEquals("test-photo.png", pngPhoto.getOriginalFilename());
        
        // Vérifier sur disque
        Path pngPath = uploadPath.resolve(pngPhoto.getStorageFilename());
        assertTrue(Files.exists(pngPath));
        
        // Nettoyage
        Files.deleteIfExists(pngPath);
    }

    @Test
    @DisplayName("Test upload de plusieurs photos par le même user")
    void testMultipleUploads() throws IOException {
        // Upload première photo
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "photo1.jpg",
                "image/jpeg",
                "Photo 1 content".getBytes()
        );
        PhotoUploadDTO dto1 = createUploadDTO("Photo 1", "Description 1", file1);
        PhotoDTO photo1 = photoService.uploadPhoto(dto1, testUser);

        // Upload deuxième photo
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "photo2.jpg",
                "image/jpeg",
                "Photo 2 content".getBytes()
        );
        PhotoUploadDTO dto2 = createUploadDTO("Photo 2", "Description 2", file2);
        PhotoDTO photo2 = photoService.uploadPhoto(dto2, testUser);

        // Vérifications
        assertNotNull(photo1.getId());
        assertNotNull(photo2.getId());
        assertNotEquals(photo1.getId(), photo2.getId(), "Les photos doivent avoir des IDs différents");
        assertNotEquals(photo1.getStorageFilename(), photo2.getStorageFilename(), 
                "Les noms de fichiers de stockage doivent être uniques");

        // Vérifier que les deux fichiers existent
        Path path1 = uploadPath.resolve(photo1.getStorageFilename());
        Path path2 = uploadPath.resolve(photo2.getStorageFilename());
        assertTrue(Files.exists(path1), "Le premier fichier doit exister");
        assertTrue(Files.exists(path2), "Le second fichier doit exister");

        // Vérifier en BDD
        List<Photo> userPhotos = photoRepository.findAll();
        assertTrue(userPhotos.size() >= 2, "L'user doit avoir au moins 2 photos en BDD");

        // Nettoyage
        Files.deleteIfExists(path1);
        Files.deleteIfExists(path2);
    }

    @Test
    @DisplayName("Test upload avec visibilité PUBLIC")
    void testUploadPublicPhoto() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "public-photo.jpg",
                "image/jpeg",
                createTestImageContent()
        );

        PhotoUploadDTO uploadDTO = new PhotoUploadDTO();
        uploadDTO.setTitle("Photo publique");
        uploadDTO.setDescription("Cette photo est publique");
        uploadDTO.setVisibility("PUBLIC");
        uploadDTO.setFile(file);

        PhotoDTO uploadedPhoto = photoService.uploadPhoto(uploadDTO, testUser);

        // Vérifications
        assertEquals("PUBLIC", uploadedPhoto.getVisibility());

        Photo photoInDb = photoRepository.findById(uploadedPhoto.getId()).orElse(null);
        assertNotNull(photoInDb);
        assertEquals(Photo.Visibility.PUBLIC, photoInDb.getVisibility());

        // Nettoyage
        Path uploadedPath = uploadPath.resolve(uploadedPhoto.getStorageFilename());
        Files.deleteIfExists(uploadedPath);
    }

    @Test
    @DisplayName("Test que le nom de fichier de stockage est unique (UUID)")
    void testStorageFilenameIsUnique() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "same-name.jpg",
                "image/jpeg",
                "Content 1".getBytes()
        );
        
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "same-name.jpg",  // Même nom original
                "image/jpeg",
                "Content 2".getBytes()
        );

        PhotoUploadDTO dto1 = createUploadDTO("Photo A", "Description A", file1);
        PhotoUploadDTO dto2 = createUploadDTO("Photo B", "Description B", file2);

        PhotoDTO photo1 = photoService.uploadPhoto(dto1, testUser);
        PhotoDTO photo2 = photoService.uploadPhoto(dto2, testUser);

        // Les noms de stockage doivent être différents même si le nom original est le même
        assertNotEquals(photo1.getStorageFilename(), photo2.getStorageFilename(),
                "Les noms de fichiers de stockage doivent être uniques");
        
        assertEquals("same-name.jpg", photo1.getOriginalFilename());
        assertEquals("same-name.jpg", photo2.getOriginalFilename());

        // Nettoyage
        Files.deleteIfExists(uploadPath.resolve(photo1.getStorageFilename()));
        Files.deleteIfExists(uploadPath.resolve(photo2.getStorageFilename()));
    }

    @Test
    @DisplayName("Test récupération du fichier après upload")
    void testRetrieveFileAfterUpload() throws IOException {
        // Upload
        byte[] originalContent = "Test file content for retrieval".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "retrieve-test.jpg",
                "image/jpeg",
                originalContent
        );

        PhotoUploadDTO uploadDTO = createUploadDTO("Photo à récupérer", "Test", file);
        PhotoDTO uploadedPhoto = photoService.uploadPhoto(uploadDTO, testUser);

        byte[] retrievedContent = photoService.getPhotoFile(uploadedPhoto.getId());

        assertArrayEquals(originalContent, retrievedContent,
                "Le contenu récupéré doit correspondre au contenu original");

        Files.deleteIfExists(uploadPath.resolve(uploadedPhoto.getStorageFilename()));
    }

    // ===== Méthodes utilitaires =====

    private byte[] createTestImageContent() {
        return "Test image content with some binary-like data...".getBytes();
    }

    private PhotoUploadDTO createUploadDTO(String title, String description, MockMultipartFile file) {
        PhotoUploadDTO dto = new PhotoUploadDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setVisibility("PRIVATE");
        dto.setFile(file);
        return dto;
    }
}

