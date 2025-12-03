package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.dto.PhotoUploadDTO;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.mapper.PhotoMapper;
import local.epul4a.fotoshare.repository.ShareRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final SecurityService securityService;
    private final ShareRepository shareRepository;
    private final PhotoMapper photoMapper;

    @Value("${fotoshare.upload.directory:uploads}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int THUMBNAIL_SIZE = 300;

    public PhotoService(PhotoRepository photoRepository, SecurityService securityService, ShareRepository shareRepository, PhotoMapper photoMapper) {
        this.photoRepository = photoRepository;
        this.securityService = securityService;
        this.shareRepository = shareRepository;
        this.photoMapper = photoMapper;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Fichier trop volumineux");
        }

        String declaredContentType = file.getContentType();
        if (declaredContentType == null || !ALLOWED_TYPES.contains(declaredContentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé");
        }

        Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
        try {
            file.transferTo(tempFile);
            String detectedContentType = Files.probeContentType(tempFile);

            if (detectedContentType == null || !ALLOWED_TYPES.contains(detectedContentType)) {
                throw new IllegalArgumentException("Le contenu du fichier ne correspond pas à un type d'image autorisé");
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String generateFilename(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID() + extension;
    }

    private void createThumbnail(Path source, Path dest) throws IOException {
        BufferedImage originalImage = ImageIO.read(source.toFile());
        if (originalImage == null) return;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int newWidth, newHeight;

        if (width > height) {
            newWidth = THUMBNAIL_SIZE;
            newHeight = (int) ((double) height / width * THUMBNAIL_SIZE);
        } else {
            newHeight = THUMBNAIL_SIZE;
            newWidth = (int) ((double) width / height * THUMBNAIL_SIZE);
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        ImageIO.write(resizedImage, "jpg", dest.toFile());
    }

    public PhotoDTO uploadPhoto(PhotoUploadDTO dto, User owner) throws IOException {
        validateFile(dto.getFile());

        String storageFilename = generateFilename(dto.getFile().getOriginalFilename());
        String thumbnailFilename = "thumb_" + storageFilename;

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.write(uploadPath.resolve(storageFilename), dto.getFile().getBytes());

        try {
            createThumbnail(uploadPath.resolve(storageFilename), uploadPath.resolve(thumbnailFilename));
        } catch (Exception e) {
            thumbnailFilename = storageFilename;
        }

        Photo photo = new Photo();
        photo.setTitle(dto.getTitle());
        photo.setDescription(dto.getDescription());
        photo.setOriginalFilename(dto.getFile().getOriginalFilename());
        photo.setStorageFilename(storageFilename);
        photo.setThumbnailFilename(thumbnailFilename);
        photo.setContentType(dto.getFile().getContentType());
        photo.setVisibility(Photo.Visibility.valueOf(dto.getVisibility()));
        photo.setOwner(owner);

        return photoMapper.toDTO(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public Photo findById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public PhotoDTO getPhotoDTO(Long id) {
        return photoMapper.toDTO(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<PhotoDTO> getPhotosByOwner(User owner, Pageable pageable) {
        return photoRepository.findByOwner(owner, pageable).map(photoMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PhotoDTO> getAccessiblePhotos(User user, Pageable pageable) {
        return photoRepository.findAccessiblePhotos(user, pageable).map(photoMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PhotoDTO> getPublicPhotos(Pageable pageable) {
        return photoRepository.findByVisibility(Photo.Visibility.PUBLIC, pageable).map(photoMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PhotoDTO> getSharedWithUser(User user, Pageable pageable) {
        return photoRepository.findSharedWithUser(user, pageable).map(photoMapper::toDTO);
    }

    public void deletePhoto(Long id, User user) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        
        if (user.getRole() == User.Role.ADMIN) {
            deletePhotoFiles(photo);
            photoRepository.delete(photo);
            return;
        }

        if (securityService.isOwner(user, photo)) {
            deletePhotoFiles(photo);
            photoRepository.delete(photo);
            return;
        }
        
        if (user.getRole() == User.Role.MODERATOR) {
            if (photo.getVisibility() == Photo.Visibility.PUBLIC ||
                shareRepository.existsByPhotoAndUser(photo, user)) {
                deletePhotoFiles(photo);
                photoRepository.delete(photo);
                return;
            }
        }

        throw new RuntimeException("Accès refusé");
    }

    public void updatePhoto(Long id, PhotoUploadDTO dto, User user) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        
        if (!securityService.isOwner(user, photo)) {
            throw new RuntimeException("Accès refusé");
        }

        photo.setTitle(dto.getTitle());
        photo.setDescription(dto.getDescription());
        photo.setVisibility(Photo.Visibility.valueOf(dto.getVisibility()));
        photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public byte[] getPhotoFile(Long id) throws IOException {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        return Files.readAllBytes(Paths.get(uploadDirectory).resolve(photo.getStorageFilename()));
    }

    @Transactional(readOnly = true)
    public byte[] getThumbnailFile(Long id) throws IOException {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        Path thumbnailPath = Paths.get(uploadDirectory).resolve(photo.getThumbnailFilename());

        if (Files.exists(thumbnailPath)) {
            return Files.readAllBytes(thumbnailPath);
        }
        return Files.readAllBytes(Paths.get(uploadDirectory).resolve(photo.getStorageFilename()));
    }

    private void deletePhotoFiles(Photo photo) {
        try {
            Files.deleteIfExists(Paths.get(uploadDirectory).resolve(photo.getStorageFilename()));
            if (photo.getThumbnailFilename() != null) {
                Files.deleteIfExists(Paths.get(uploadDirectory).resolve(photo.getThumbnailFilename()));
            }
        } catch (IOException ignored) {
        }
    }

    @Transactional(readOnly = true)
    public long countAllPhotos() {
        return photoRepository.count();
    }

    @Transactional(readOnly = true)
    public Page<PhotoDTO> getAllPhotos(Pageable pageable) {
        return photoRepository.findAll(pageable).map(photoMapper::toDTO);
    }

    public void deletePhotoByAdmin(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        deletePhotoFiles(photo);
        photoRepository.delete(photo);
    }
}

