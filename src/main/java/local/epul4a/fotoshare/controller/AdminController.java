package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.PhotoDTO;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.service.PhotoService;
import local.epul4a.fotoshare.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final PhotoService photoService;

    public AdminController(UserService userService, PhotoService photoService) {
        this.userService = userService;
        this.photoService = photoService;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        long totalUsers = userService.countAllUsers();
        long totalPhotos = photoService.countAllPhotos();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPhotos", totalPhotos);
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           Model model) {
        Page<User> users = userService.getAllUsers(PageRequest.of(page, size));
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/photos")
    public String listPhotos(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        Page<PhotoDTO> photos = photoService.getAllPhotos(PageRequest.of(page, size));
        model.addAttribute("photos", photos);
        return "admin/photos";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id,
                                 @RequestParam String role,
                                 RedirectAttributes redirectAttributes) {
        try {
            User.Role newRole = User.Role.valueOf(role);

            if (newRole == User.Role.ADMIN) {
                redirectAttributes.addFlashAttribute("error", "Impossible de promouvoir un user en ADMIN via l'interface web");
                return "redirect:/admin/users";
            }

            userService.changeUserRole(id, newRole);
            redirectAttributes.addFlashAttribute("success", "Rôle modifié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification du rôle");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User supprimé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleUserEnabled(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserEnabled(id);
            redirectAttributes.addFlashAttribute("success", "Statut de l'user modifié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification du statut: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/photos/{id}/delete")
    public String deletePhoto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            photoService.deletePhotoByAdmin(id);
            redirectAttributes.addFlashAttribute("success", "Photo supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression");
        }
        return "redirect:/admin/photos";
    }
}

