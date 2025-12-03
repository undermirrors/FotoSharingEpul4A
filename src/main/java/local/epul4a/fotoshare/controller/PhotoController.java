package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.AlbumDTO;
import local.epul4a.fotoshare.dto.CommentDTO;
import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.dto.PhotoUploadDTO;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.service.AlbumService;
import local.epul4a.fotoshare.service.CommentService;
import local.epul4a.fotoshare.service.PhotoService;
import local.epul4a.fotoshare.service.SecurityService;
import local.epul4a.fotoshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/photos")
public class PhotoController {

    private final PhotoService photoService;
    private final UserService userService;
    private final SecurityService securityService;
    private final CommentService commentService;
    private final AlbumService albumService;

    public PhotoController(PhotoService photoService,
                          UserService userService,
                          SecurityService securityService,
                          CommentService commentService,
                          AlbumService albumService) {
        this.photoService = photoService;
        this.userService = userService;
        this.securityService = securityService;
        this.commentService = commentService;
        this.albumService = albumService;
    }

    @GetMapping
    public String listPhotos(Authentication authentication,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "12") int size,
                             Model model) {
        User user = userService.findByUsername(authentication.getName());
        Page<PhotoDTO> photos = photoService.getAccessiblePhotos(user, PageRequest.of(page, size));
        model.addAttribute("photos", photos);
        model.addAttribute("pageTitle", "Toutes les photos accessibles");
        model.addAttribute("authenticated", true);
        model.addAttribute("baseUrl", "/photos");
        return "photos/generic-list";
    }

    @GetMapping("/my")
    public String myPhotos(Authentication authentication,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size,
                           Model model) {
        User user = userService.findByUsername(authentication.getName());
        Page<PhotoDTO> photos = photoService.getPhotosByOwner(user, PageRequest.of(page, size));
        model.addAttribute("photos", photos);
        model.addAttribute("pageTitle", "Mes photos");
        model.addAttribute("authenticated", true);
        model.addAttribute("baseUrl", "/photos/my");
        return "photos/generic-list";
    }

    @GetMapping("/public")
    public String publicPhotos(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size,
                               Model model) {
        Page<PhotoDTO> photos = photoService.getPublicPhotos(PageRequest.of(page, size));
        model.addAttribute("photos", photos);
        model.addAttribute("pageTitle", "Photos publiques");
        model.addAttribute("authenticated", false);
        model.addAttribute("baseUrl", "/photos/public");
        return "photos/generic-list";
    }

    @GetMapping("/shared")
    public String sharedPhotos(Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Model model) {
        User user = userService.findByUsername(authentication.getName());
        Page<PhotoDTO> photos = photoService.getSharedWithUser(user, PageRequest.of(page, size));
        model.addAttribute("photos", photos);
        model.addAttribute("pageTitle", "Photos Partagées avec Moi");
        model.addAttribute("authenticated", true);
        model.addAttribute("baseUrl", "/photos/shared");
        return "photos/generic-list";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("photo", new PhotoUploadDTO());
        return "photos/upload";
    }

    @PostMapping("/upload")
    public String uploadPhoto(@Valid @ModelAttribute("photo") PhotoUploadDTO dto,
                              BindingResult result,
                              Authentication authentication,
                              Model model) {
        if (dto.getFile() == null || dto.getFile().isEmpty()) {
            result.rejectValue("file", "error.file", "Veuillez sélectionner un fichier");
        }

        if (result.hasErrors()) {
            return "photos/upload";
        }

        try {
            User user = userService.findByUsername(authentication.getName());

            photoService.uploadPhoto(dto, user);
            return "redirect:/photos/my?uploaded=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "photos/upload";
        } catch (IOException e) {
            model.addAttribute("error", "Erreur lors de l'upload: " + e.getMessage());
            return "photos/upload";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessPhoto(authentication, #id)")
    public String viewPhoto(@PathVariable Long id, Authentication authentication, Model model) {
        PhotoDTO photo = photoService.getPhotoDTO(id);
        Page<CommentDTO> comments = commentService.getPhotoComments(id, PageRequest.of(0, 50));

        List<AlbumDTO> userAlbums = null;
        List<AlbumDTO> albumsContainingPhoto = null;
        final User currentUser;

        if (authentication != null) {
            currentUser = userService.findByUsername(authentication.getName());
            userAlbums = albumService.getAlbumsWithoutPhoto(currentUser, id);
            // Ne montrer que les albums dont l'user connecté est propriétaire
            List<AlbumDTO> allAlbumsContainingPhoto = albumService.getAlbumsContainingPhoto(id);
            if (allAlbumsContainingPhoto != null) {
                final Long userId = currentUser.getId();
                albumsContainingPhoto = allAlbumsContainingPhoto.stream()
                    .filter(album -> album.getOwnerId().equals(userId))
                    .toList();
            }
        } else {
            currentUser = null;
        }

        model.addAttribute("photo", photo);
        model.addAttribute("comments", comments.getContent());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userAlbums", userAlbums);
        model.addAttribute("albumsContainingPhoto", albumsContainingPhoto);
        return "photos/view";
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id, Authentication authentication) {
        try {
            if (!securityService.canAccessPhoto(authentication, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            byte[] imageBytes = photoService.getPhotoFile(id);
            PhotoDTO photo = photoService.getPhotoDTO(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(photo.getContentType()));

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id, Authentication authentication) {
        try {
            if (!securityService.canAccessPhoto(authentication, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            PhotoDTO photo = photoService.getPhotoDTO(id);
            byte[] imageBytes = photoService.getThumbnailFile(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(photo.getContentType()));

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@securityService.canEditPhoto(authentication, #id)")
    public String showEditForm(@PathVariable Long id, Model model) {
        PhotoDTO photo = photoService.getPhotoDTO(id);
        PhotoUploadDTO dto = new PhotoUploadDTO();
        dto.setTitle(photo.getTitle());
        dto.setDescription(photo.getDescription());
        dto.setVisibility(photo.getVisibility());
        model.addAttribute("photo", dto);
        model.addAttribute("photoId", id);
        return "photos/edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("@securityService.canEditPhoto(authentication, #id)")
    public String editPhoto(@PathVariable Long id,
                            @Valid @ModelAttribute("photo") PhotoUploadDTO dto,
                            BindingResult result,
                            Authentication authentication,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("photoId", id);
            return "photos/edit";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            photoService.updatePhoto(id, dto, user);
            return "redirect:/photos/" + id + "?updated=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("photoId", id);
            return "photos/edit";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("@securityService.canDeletePhoto(authentication, #id)")
    public String deletePhoto(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        photoService.deletePhoto(id, user);
        return "redirect:/photos/my?deleted=true";
    }
}
