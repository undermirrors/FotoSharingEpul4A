package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.ShareDTO;
import local.epul4a.fotoshare.entity.Share;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.service.ShareService;
import local.epul4a.fotoshare.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/photos/{photoId}/shares")
public class ShareController {

    private final ShareService shareService;
    private final UserService userService;

    public ShareController(ShareService shareService,
                             UserService userService) {
        this.shareService = shareService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@securityService.canEditPhoto(authentication, #photoId)")
    public String listShares(@PathVariable Long photoId,
                             Authentication authentication,
                             Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<ShareDTO> shares = shareService.getPhotoShares(photoId, user);

        model.addAttribute("shares", shares);
        model.addAttribute("photoId", photoId);
        return "shares/list";
    }

    @PostMapping
    @PreAuthorize("@securityService.canEditPhoto(authentication, #photoId)")
    public String sharePhoto(@PathVariable Long photoId,
                             @RequestParam String username,
                             @RequestParam String permissionLevel,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User owner = userService.findByUsername(authentication.getName());


            User targetUser;
            try {
                targetUser = userService.findByUsername(username);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "User '" + username + "' introuvable");
                return "redirect:/photos/" + photoId + "/shares";
            }

            if (targetUser.getId().equals(owner.getId())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas partager une photo avec vous-même");
                return "redirect:/photos/" + photoId + "/shares";
            }

            shareService.sharePhoto(photoId, targetUser.getId(), Share.PermissionLevel.valueOf(permissionLevel), owner);
            redirectAttributes.addFlashAttribute("success", "Photo partagée avec succès avec " + username);
            return "redirect:/photos/" + photoId + "/shares";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/photos/" + photoId + "/shares";
        }
    }

    @PostMapping("/{partageId}/update")
    @PreAuthorize("@securityService.canEditPhoto(authentication, #photoId)")
    public String updatePermission(@PathVariable Long photoId,
                                   @PathVariable Long partageId,
                                   @RequestParam String permissionLevel,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User owner = userService.findByUsername(authentication.getName());
            shareService.updatePermission(partageId, Share.PermissionLevel.valueOf(permissionLevel), owner);
            redirectAttributes.addFlashAttribute("success", "Permissions mises à jour");
            return "redirect:/photos/" + photoId + "/shares?updated=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/photos/" + photoId + "/shares";
        }
    }

    @PostMapping("/{partageId}/revoke")
    @PreAuthorize("@securityService.canEditPhoto(authentication, #photoId)")
    public String revokeAccess(@PathVariable Long photoId,
                               @PathVariable Long partageId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User owner = userService.findByUsername(authentication.getName());
            shareService.revokeAccess(partageId, owner);
            redirectAttributes.addFlashAttribute("success", "Accès révoqué");
            return "redirect:/photos/" + photoId + "/shares?revoked=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/photos/" + photoId + "/shares";
        }
    }
}

