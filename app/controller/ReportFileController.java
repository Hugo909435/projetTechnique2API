package app.controller;

import app.dto.ReportFileResponseDto;
import app.service.ReportFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ReportFileController {

    private final ReportFileService fileService;

    public ReportFileController(ReportFileService fileService) {
        this.fileService = fileService;
    }

    /** Upload d'un fichier — rapport modifiable uniquement. */
    @PostMapping("/api/reports/{reportId}/files")
    public ResponseEntity<ReportFileResponseDto> upload(
            @PathVariable Long reportId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.upload(reportId, file, auth.getName()));
    }

    /** Liste des fichiers d'un rapport. */
    @GetMapping("/api/reports/{reportId}/files")
    public ResponseEntity<List<ReportFileResponseDto>> listFiles(
            @PathVariable Long reportId,
            Authentication auth) {
        return ResponseEntity.ok(fileService.listFiles(reportId, auth.getName()));
    }

    /** Téléchargement sécurisé d'un fichier. */
    @GetMapping("/api/report-files/{fileId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long fileId,
            Authentication auth) {
        return fileService.download(fileId, auth.getName());
    }

    /** Suppression d'un fichier — rapport modifiable uniquement. */
    @DeleteMapping("/api/report-files/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long fileId,
            Authentication auth) {
        fileService.deleteFile(fileId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
