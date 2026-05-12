package app.service;

import app.config.FileStorageProperties;
import app.dto.ReportFileResponseDto;
import app.exception.ResourceNotFoundException;
import app.model.MonthlyReport;
import app.model.ReportFile;
import app.model.User;
import app.repository.MonthlyReportRepository;
import app.repository.ReportFileRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReportFileService {

    private final ReportFileRepository fileRepository;
    private final MonthlyReportRepository reportRepository;
    private final UserService userService;
    private final ReportPermissionService permissionService;
    private final FileStorageProperties storageProps;

    public ReportFileService(ReportFileRepository fileRepository,
                             MonthlyReportRepository reportRepository,
                             UserService userService,
                             ReportPermissionService permissionService,
                             FileStorageProperties storageProps) {
        this.fileRepository = fileRepository;
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.permissionService = permissionService;
        this.storageProps = storageProps;
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    public ReportFileResponseDto upload(Long reportId, MultipartFile file, String uploaderEmail) {
        User uploader = userService.requireUser(uploaderEmail);
        MonthlyReport report = requireReport(reportId);
        permissionService.assertCanEdit(report, uploader);

        validateFile(file);

        String ext = getExtension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path dest = Paths.get(storageProps.getUploadPath(), String.valueOf(reportId), storedFilename);

        try {
            Files.createDirectories(dest.getParent());
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }

        ReportFile saved = fileRepository.save(ReportFile.builder()
                .report(report)
                .uploadedBy(uploader)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .storagePath(dest.toAbsolutePath().toString())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .build());

        return toDto(saved);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReportFileResponseDto> listFiles(Long reportId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        MonthlyReport report = requireReport(reportId);
        permissionService.assertCanRead(report, requester);
        return fileRepository.findByReportIdOrderByCreatedAtDesc(reportId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long fileId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        ReportFile file = requireFile(fileId);
        permissionService.assertCanRead(file.getReport(), requester);

        Resource resource = new FileSystemResource(Paths.get(file.getStoragePath()));
        if (!resource.exists())
            throw new ResourceNotFoundException("Fichier physique introuvable");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource);
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    public void deleteFile(Long fileId, String requesterEmail) {
        User requester = userService.requireUser(requesterEmail);
        ReportFile file = requireFile(fileId);
        permissionService.assertCanEdit(file.getReport(), requester);

        try {
            Files.deleteIfExists(Paths.get(file.getStoragePath()));
        } catch (IOException ignored) {
            // disk error doesn't block DB deletion
        }
        fileRepository.delete(file);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("Le fichier est vide");

        if (file.getSize() > storageProps.getMaxSize())
            throw new IllegalArgumentException(
                    "Fichier trop volumineux (max " + storageProps.getMaxSize() / 1_048_576 + " Mo)");

        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (ext.isEmpty() || !storageProps.getAllowedExtensions().contains(ext))
            throw new IllegalArgumentException("Extension non autorisée : ." + ext);

        String ct = file.getContentType();
        if (ct == null || !storageProps.getAllowedContentTypes().contains(ct))
            throw new IllegalArgumentException("Type de contenu non autorisé : " + ct);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private MonthlyReport requireReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : " + id));
    }

    private ReportFile requireFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fichier introuvable : " + id));
    }

    private ReportFileResponseDto toDto(ReportFile f) {
        return new ReportFileResponseDto(
                f.getId(),
                f.getOriginalFilename(),
                f.getContentType(),
                f.getSize(),
                f.getUploadedBy().getFirstName() + " " + f.getUploadedBy().getLastName(),
                f.getCreatedAt());
    }
}
