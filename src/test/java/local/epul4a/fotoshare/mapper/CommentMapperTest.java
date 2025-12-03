package local.epul4a.fotoshare.mapper;

import local.epul4a.fotoshare.dto.CommentDTO;
import local.epul4a.fotoshare.entity.Comment;
import local.epul4a.fotoshare.entity.Photo;
import local.epul4a.fotoshare.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du mapper Comment")
class CommentaireMapperTest {

    private CommentMapper commentMapper;
    private User testAuthor;
    private Photo testPhoto;
    private Comment testCommentaire;

    @BeforeEach
    void setUp() {
        commentMapper = new CommentMapper();

        testAuthor = new User();
        testAuthor.setId(1L);
        testAuthor.setUsername("auteur");
        testAuthor.setEmail("auteur@example.com");

        User photoOwner = new User();
        photoOwner.setId(2L);
        photoOwner.setUsername("proprietaire");

        testPhoto = new Photo();
        testPhoto.setId(100L);
        testPhoto.setTitle("Photo commentée");
        testPhoto.setOwner(photoOwner);

        testCommentaire = new Comment();
        testCommentaire.setId(10L);
        testCommentaire.setText("Superbe photo !");
        testCommentaire.setPhoto(testPhoto);
        testCommentaire.setAuthor(testAuthor);
        testCommentaire.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @DisplayName("Devrait convertir Comment vers CommentDTO correctement")
    void shouldConvertCommentaireToDTO() {
        CommentDTO dto = commentMapper.toDTO(testCommentaire);
        assertNotNull(dto, "Le DTO ne doit pas être null");
        assertEquals(testCommentaire.getId(), dto.getId(), "L'ID doit correspondre");
        assertEquals(testCommentaire.getText(), dto.getText(), "Le texte doit correspondre");
        assertEquals(testCommentaire.getPhoto().getId(), dto.getPhotoId(), "L'ID de la photo doit correspondre");
        assertEquals(testCommentaire.getAuthor().getUsername(), dto.getAuthorUsername(), "Le nom d'user de l'auteur doit correspondre");
        assertEquals(testCommentaire.getAuthor().getId(), dto.getAuthorId(), "L'ID de l'auteur doit correspondre");
    }

    @Test
    @DisplayName("Devrait gérer un comment long")
    void shouldHandleLongComment() {
        String longText = "C'est un très long comment qui contient beaucoup de texte. " +
                "Il peut s'étendre sur plusieurs lignes et contenir beaucoup d'informations. " +
                "Le système doit être capable de gérer ce type de contenu sans problème.";
        testCommentaire.setText(longText);

        CommentDTO dto = commentMapper.toDTO(testCommentaire);

        assertEquals(longText, dto.getText(), "Le texte long doit être correctement converti");
        assertTrue(dto.getText().length() > 100, "Le texte doit être long");
    }

    @Test
    @DisplayName("Devrait préserver les relations entre entités")
    void shouldPreserveEntityRelationships() {
        CommentDTO dto = commentMapper.toDTO(testCommentaire);

        assertEquals(testPhoto.getId(), dto.getPhotoId(), "La relation avec la photo doit être préservée");
        assertEquals(testAuthor.getId(), dto.getAuthorId(), "La relation avec l'auteur doit être préservée");
    }

    @Test
    @DisplayName("Devrait convertir plusieurs comments sur la même photo")
    void shouldConvertMultipleCommentsOnSamePhoto() {
        User anotherAuthor = new User();
        anotherAuthor.setId(3L);
        anotherAuthor.setUsername("autreAuteur");

        Comment anotherComment = new Comment();
        anotherComment.setId(20L);
        anotherComment.setText("Je suis d'accord !");
        anotherComment.setPhoto(testPhoto);
        anotherComment.setAuthor(anotherAuthor);

        CommentDTO dto1 = commentMapper.toDTO(testCommentaire);
        CommentDTO dto2 = commentMapper.toDTO(anotherComment);

        assertEquals(testPhoto.getId(), dto1.getPhotoId(), "Le premier comment doit référencer la même photo");
        assertEquals(testPhoto.getId(), dto2.getPhotoId(), "Le second comment doit référencer la même photo");
        assertNotEquals(dto1.getAuthorId(), dto2.getAuthorId(), "Les auteurs doivent être différents");
        assertNotEquals(dto1.getId(), dto2.getId(), "Les IDs doivent être différents");
    }

    @Test
    @DisplayName("Devrait gérer un comment avec caractères spéciaux")
    void shouldHandleSpecialCharacters() {
        testCommentaire.setText("Génial! 😊 C'est très beau 👍 <3");

        CommentDTO dto = commentMapper.toDTO(testCommentaire);

        assertEquals("Génial! 😊 C'est très beau 👍 <3", dto.getText(),
                "Les caractères spéciaux et emojis doivent être préservés");
    }

    @Test
    @DisplayName("Devrait convertir comments de différents auteurs")
    void shouldConvertCommentsFromDifferentAuthors() {
        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(20L);
        user2.setUsername("user2");

        testCommentaire.setAuthor(user1);

        Comment comment2 = new Comment();
        comment2.setId(11L);
        comment2.setText("Autre comment");
        comment2.setPhoto(testPhoto);
        comment2.setAuthor(user2);

        CommentDTO dto1 = commentMapper.toDTO(testCommentaire);
        CommentDTO dto2 = commentMapper.toDTO(comment2);

        assertEquals("user1", dto1.getAuthorUsername(), "Le username du premier auteur doit correspondre");
        assertEquals("user2", dto2.getAuthorUsername(), "Le username du second auteur doit correspondre");
        assertEquals(10L, dto1.getAuthorId(), "L'ID du premier auteur doit correspondre");
        assertEquals(20L, dto2.getAuthorId(), "L'ID du second auteur doit correspondre");
    }
}

