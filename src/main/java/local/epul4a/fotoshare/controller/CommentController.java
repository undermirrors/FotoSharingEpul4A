package local.epul4a.fotoshare.controller;

import local.epul4a.fotoshare.dto.CommentDTO;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.service.CommentService;
import local.epul4a.fotoshare.service.SecurityService;
import local.epul4a.fotoshare.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/photos/{photoId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(CommentService commentService, 
                                  UserService userService,
                                  SecurityService securityService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping
    public String listComments(@PathVariable Long photoId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        Page<CommentDTO> comments = commentService.getPhotoComments(photoId, PageRequest.of(page, size));
        model.addAttribute("comments", comments);
        model.addAttribute("photoId", photoId);
        return "comments/list";
    }

    @PostMapping
    @PreAuthorize("@securityService.canCommentPhoto(authentication, #photoId)")
    public String addComment(@PathVariable Long photoId,
                             @RequestParam String text,
                             Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());

        commentService.addComment(photoId, text, user);
        return "redirect:/photos/" + photoId + "?commented=true";
    }

    @PostMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable Long photoId,
                                @PathVariable Long commentId,
                                Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        commentService.deleteComment(commentId, user);
        return "redirect:/photos/" + photoId + "?commentDeleted=true";
    }
}

