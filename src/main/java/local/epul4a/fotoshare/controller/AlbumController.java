package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.AlbumDTO;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.service.AlbumService;
import local.epul4a.fotoshare.service.PhotoService;
import local.epul4a.fotoshare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/albums")
public class AlbumController {

    private final AlbumService albumService;
    private final UserService userService;

    public AlbumController(AlbumService albumService, UserService userService, PhotoService photoService) {
        this.albumService = albumService;
        this.userService = userService;
    }

    @GetMapping
    public String listAlbums(Authentication authentication,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "12") int size,
                            Model model) {
        User user = userService.findByUsername(authentication.getName());
        Page<AlbumDTO> albums = albumService.getAlbumsByOwner(user, PageRequest.of(page, size));
        model.addAttribute("albums", albums);
        return "albums/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("album", new AlbumDTO());
        model.addAttribute("pageTitle", "Créer un nouvel album");
        model.addAttribute("formAction", "/albums/create");
        model.addAttribute("buttonText", "Créer l'album");
        return "albums/form";
    }

    @PostMapping("/create")
    public String createAlbum(@Valid @ModelAttribute("album") AlbumDTO dto,
                             BindingResult result,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Créer un nouvel album");
            model.addAttribute("formAction", "/albums/create");
            model.addAttribute("buttonText", "Créer l'album");
            return "albums/form";
        }

        try {
            User user = userService.findByUsername(authentication.getName());

            if (!user.getEnabled()) {
                redirectAttributes.addFlashAttribute("error", "Votre compte est désactivé. Vous ne pouvez pas créer d'albums.");
                return "redirect:/albums";
            }

            AlbumDTO created = albumService.createAlbum(dto.getName(), dto.getDescription(), user);
            redirectAttributes.addFlashAttribute("success", "Album créé avec succès");
            return "redirect:/albums/" + created.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/albums";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessAlbum(authentication, #id)")
    public String viewAlbum(@PathVariable Long id,
                           Authentication authentication,
                           Model model) {
        AlbumDTO album = albumService.getAlbumDTO(id);
        User user = userService.findByUsername(authentication.getName());
        
        model.addAttribute("album", album);
        model.addAttribute("photos", albumService.getAlbumPhotos(id));
        model.addAttribute("canEdit", album.getOwnerId().equals(user.getId()));
        return "albums/view";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@securityService.canEditAlbum(authentication, #id)")
    public String showEditForm(@PathVariable Long id, Authentication authentication, Model model) {
        AlbumDTO album = albumService.getAlbumDTO(id);
        User user = userService.findByUsername(authentication.getName());
        
        if (!album.getOwnerId().equals(user.getId())) {
            return "redirect:/albums/" + id;
        }
        
        model.addAttribute("album", album);
        model.addAttribute("pageTitle", "Modifier l'album");
        model.addAttribute("formAction", "/albums/" + id + "/edit");
        model.addAttribute("buttonText", "Enregistrer");
        return "albums/form";
    }

    @PostMapping("/{id}/edit")
    public String editAlbum(@PathVariable Long id,
                           @Valid @ModelAttribute("album") AlbumDTO dto,
                           BindingResult result,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Modifier l'album");
            model.addAttribute("formAction", "/albums/" + id + "/edit");
            model.addAttribute("buttonText", "Enregistrer");
            return "albums/form";
        }

        try {
            User user = userService.findByUsername(authentication.getName());
            albumService.updateAlbum(id, dto.getName(), dto.getDescription(), user);
            redirectAttributes.addFlashAttribute("success", "Album mis à jour");
            return "redirect:/albums/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/albums/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAlbum(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            albumService.deleteAlbum(id, user);
            redirectAttributes.addFlashAttribute("success", "Album supprimé");
            return "redirect:/albums";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/albums/" + id;
        }
    }

    @PostMapping("/{albumId}/photos/{photoId}/add")
    public String addPhotoToAlbum(@PathVariable Long albumId,
                                 @PathVariable Long photoId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            albumService.addPhotoToAlbum(albumId, photoId, user);
            redirectAttributes.addFlashAttribute("success", "Photo ajoutée à l'album");
            return "redirect:/albums/" + albumId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/photos/" + photoId;
        }
    }

    @PostMapping("/add-photo")
    public String addPhotoToAlbumFromForm(@RequestParam Long albumId,
                                         @RequestParam Long photoId,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());

            albumService.addPhotoToAlbum(albumId, photoId, user);
            redirectAttributes.addFlashAttribute("success", "Photo ajoutée à l'album");
            return "redirect:/photos/" + photoId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/photos/" + photoId;
        }
    }

    @PostMapping("/{albumId}/photos/{photoId}/remove")
    @PreAuthorize("@securityService.canEditAlbum(authentication, #albumId)")
    public String removePhotoFromAlbum(@PathVariable Long albumId,
                                      @PathVariable Long photoId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(authentication.getName());
            albumService.removePhotoFromAlbum(albumId, photoId, user);
            redirectAttributes.addFlashAttribute("success", "Photo retirée de l'album");
            return "redirect:/albums/" + albumId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/albums/" + albumId;
        }
    }
}

