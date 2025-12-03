package local.epul4a.fotoshare.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
        logger.error("Fichier trop volumineux", ex);
        redirectAttributes.addFlashAttribute("error", "Le fichier est trop volumineux. Taille maximale autorisée : 10MB");
        return "redirect:/photos/upload";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, RedirectAttributes redirectAttributes) {
        logger.warn("Accès refusé", ex);
        redirectAttributes.addFlashAttribute("error", "Vous n'avez pas les droits nécessaires pour accéder à cette ressource");
        return "redirect:/photos";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        logger.error("Argument invalide", ex);
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/photos";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, RedirectAttributes redirectAttributes) {
        logger.error("Erreur d'exécution", ex);
        redirectAttributes.addFlashAttribute("error", "Une erreur s'est produite : " + ex.getMessage());
        return "redirect:/photos";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
        logger.error("Erreur inattendue", ex);
        redirectAttributes.addFlashAttribute("error", "Une erreur inattendue s'est produite");
        return "redirect:/photos";
    }
}

