package sn.uasz.voteuasz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.candidacies.FileStorageService;
import sn.uasz.voteuasz.common.BusinessException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FileSecurityAndComplaintsTests {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("Test 22 : Blocage strict des téléversements de fichiers malveillants ou interdits (.exe, .sh)")
    void testForbiddenFileUploadsBlocked() {
        // Tentative de téléversement d'un script bash ou exécutable
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "malicious executable bytes".getBytes()
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                fileStorageService.storeFile(exeFile));
        assertTrue(ex.getMessage().contains("Format de fichier non autorisé") || ex.getMessage().contains("non conforme"));

        MockMultipartFile scriptFile = new MockMultipartFile(
                "file",
                "hack.sh",
                "application/x-sh",
                "echo 'hacked'".getBytes()
        );

        assertThrows(BusinessException.class, () ->
                fileStorageService.storeFile(scriptFile));
    }

    @Test
    @DisplayName("Test 22 (bis) : Acceptation des fichiers autorisés (PDF, PNG, JPG)")
    void testAllowedFileUploads() {
        MockMultipartFile validPdf = new MockMultipartFile(
                "file",
                "justificatif_etudiant.pdf",
                "application/pdf",
                "%PDF-1.4 dummy valid pdf content".getBytes()
        );

        String filename = fileStorageService.storeFile(validPdf);
        assertNotNull(filename);
        assertTrue(filename.endsWith(".pdf"));
    }
}
