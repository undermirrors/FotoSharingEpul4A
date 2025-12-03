package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.dto.AlbumDTO;
import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.entity.Album;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.mapper.AlbumMapper;
import local.epul4a.fotoshare.mapper.PhotoMapper;
import local.epul4a.fotoshare.repository.AlbumRepository;
import local.epul4a.fotoshare.repository.PhotoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final SecurityService securityService;
    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;

    public AlbumService(AlbumRepository albumRepository, PhotoRepository photoRepository, SecurityService securityService, AlbumMapper albumMapper, PhotoMapper photoMapper) {
        this.albumRepository = albumRepository;
        this.photoRepository = photoRepository;
        this.securityService = securityService;
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
    }

    public AlbumDTO createAlbum(String name, String description, User owner) {
        Album album = new Album();
        album.setName(name);
        album.setDescription(description);
        album.setOwner(owner);
        return albumMapper.toDTO(albumRepository.save(album));
    }

    @Transactional(readOnly = true)
    public AlbumDTO getAlbumDTO(Long id) {
        return albumMapper.toDTO(albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album introuvable")));
    }

    @Transactional(readOnly = true)
    public Page<AlbumDTO> getAlbumsByOwner(User owner, Pageable pageable) {
        return albumRepository.findByOwner(owner, pageable).map(albumMapper::toDTO);
    }

    public void updateAlbum(Long id, String name, String description, User user) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album introuvable"));

        if (!album.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        album.setName(name);
        album.setDescription(description);
        albumRepository.save(album);
    }

    public void deleteAlbum(Long id, User user) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album introuvable"));

        if (!album.getOwner().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }
        albumRepository.delete(album);
    }

    public void addPhotoToAlbum(Long albumId, Long photoId, User user) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album introuvable"));

        if (!album.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));

        if (!securityService.isOwner(user, photo)) {
            throw new RuntimeException("Vous ne pouvez ajouter que vos propres photos");
        }

        if (!album.getPhotos().contains(photo)) {
            album.getPhotos().add(photo);
            albumRepository.save(album);
        }
    }

    public void removePhotoFromAlbum(Long albumId, Long photoId, User user) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album introuvable"));

        if (!album.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        album.getPhotos().remove(photo);
        albumRepository.save(album);
    }

    @Transactional(readOnly = true)
    public List<PhotoDTO> getAlbumPhotos(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album introuvable"));
        return album.getPhotos().stream()
                .map(photoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlbumDTO> getAlbumsWithoutPhoto(User owner, Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        return albumRepository.findByOwner(owner, Pageable.unpaged())
                .stream()
                .filter(album -> !album.getPhotos().contains(photo))
                .map(albumMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlbumDTO> getAlbumsContainingPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo introuvable"));
        return albumRepository.findAll()
                .stream()
                .filter(album -> album.getPhotos().contains(photo))
                .map(albumMapper::toDTO)
                .collect(Collectors.toList());
    }
}

