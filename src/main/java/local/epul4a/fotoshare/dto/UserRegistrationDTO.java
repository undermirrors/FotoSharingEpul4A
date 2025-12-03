package local.epul4a.fotoshare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    @NotEmpty(message = "Le nom d'user ne doit pas être vide")
    @Size(min = 3, max = 50, message = "Le nom d'user doit contenir entre 3 et 50 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Le nom d'user ne peut contenir que des lettres, chiffres, tirets et underscores")
    private String username;

    @NotEmpty(message = "L'email ne doit pas être vide")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotEmpty(message = "Le mot de passe ne doit pas être vide")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$", 
             message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial")
    private String password;

    @NotEmpty(message = "La confirmation du mot de passe ne doit pas être vide")
    private String confirmPassword;
}

