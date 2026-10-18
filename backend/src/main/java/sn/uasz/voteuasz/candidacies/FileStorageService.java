package sn.uasz.voteuasz.candidacies;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "application/x-pdf",
            "image/png",
            "image/x-png",
            "image/jpeg",
            "image/jpg",
            "image/pjpeg"
    );

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le répertoire de stockage des fichiers", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Échec du téléversement : le fichier est vide");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // Protection contre les attaques de chemin (Path Traversal)
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException("Nom de fichier non sécurisé ou invalide");
        }

        // Vérification de l'extension
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BusinessException("Format de fichier non autorisé. Formats acceptés : PDF, PNG, JPG");
        }

        // Vérification du type MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Type de contenu non conforme aux critères de sécurité");
        }

        // Génération d'un nom de stockage aléatoire sécurisé
        String storageFilename = UUID.randomUUID() + fileExtension;
        Path destinationFile = this.rootLocation.resolve(storageFilename).normalize().toAbsolutePath();

        // Vérification que le fichier de destination réside bien dans le répertoire cible
        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new BusinessException("Tentative de téléversement en dehors du répertoire désigné");
        }

        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storageFilename;
        } catch (IOException e) {
            throw new BusinessException("Erreur lors de l'enregistrement physique du fichier", e);
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Fichier introuvable : " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Fichier introuvable : " + filename);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
