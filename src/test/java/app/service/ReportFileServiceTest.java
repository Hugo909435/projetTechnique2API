package app.service;

import app.config.FileStorageProperties;
import app.dto.ReportFileResponseDto;
import app.exception.ForbiddenException;
import app.exception.ResourceNotFoundException;
import app.model.*;
import app.repository.MonthlyReportRepository;
import app.repository.ReportFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportFileServiceTest {

    @TempDir
    Path tempDir;

    @Mock ReportFileRepository fileRepository;
    @Mock MonthlyReportRepository reportRepository;
    @Mock UserService userService;
    @Mock ReportPermissionService permissionService;

    ReportFileService service;

    private User student;
    private MonthlyReport draftReport;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadPath(tempDir.toString());
        props.setMaxSize(10_485_760L);
        props.setAllowedExtensions(List.of("pdf", "txt"));
        props.setAllowedContentTypes(List.of("application/pdf", "text/plain"));

        service = new ReportFileService(fileRepository, reportRepository, userService, permissionService, props);

        student = User.builder().id(1L).email("student@test.com").role(Role.STUDENT)
                .firstName("Alice").lastName("Durand").password("hash").build();

        draftReport = new MonthlyReport();
        draftReport.setId(10L);
        draftReport.setStudent(student);
        draftReport.setYear(2024);
        draftReport.setMonth(5);
        draftReport.setStatus(ReportStatus.DRAFT);
        draftReport.setCreatedAt(LocalDateTime.now());
        draftReport.setUpdatedAt(LocalDateTime.now());
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    @Nested
    class Upload {

        @Test
        void stores_file_and_returns_dto() {
            MockMultipartFile multipart = new MockMultipartFile(
                    "file", "rapport.pdf", "application/pdf", "content".getBytes());

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);

            ReportFile saved = ReportFile.builder()
                    .id(1L).report(draftReport).uploadedBy(student)
                    .originalFilename("rapport.pdf").storedFilename("uuid.pdf")
                    .storagePath(tempDir.resolve("10/uuid.pdf").toString())
                    .contentType("application/pdf").size(7L)
                    .createdAt(LocalDateTime.now()).build();
            when(fileRepository.save(any())).thenReturn(saved);

            ReportFileResponseDto result = service.upload(10L, multipart, "student@test.com");

            assertThat(result).isNotNull();
            assertThat(result.originalFilename()).isEqualTo("rapport.pdf");
            assertThat(result.contentType()).isEqualTo("application/pdf");
            verify(fileRepository).save(any(ReportFile.class));
        }

        @Test
        void rejects_disallowed_extension() {
            MockMultipartFile multipart = new MockMultipartFile(
                    "file", "script.exe", "application/octet-stream", "content".getBytes());

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);

            assertThatThrownBy(() -> service.upload(10L, multipart, "student@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Extension non autorisée");
        }

        @Test
        void rejects_disallowed_content_type() {
            MockMultipartFile multipart = new MockMultipartFile(
                    "file", "photo.pdf", "image/gif", "content".getBytes());

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);

            assertThatThrownBy(() -> service.upload(10L, multipart, "student@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Type de contenu non autorisé");
        }

        @Test
        void rejects_oversized_file() {
            byte[] big = new byte[11 * 1024 * 1024];
            MockMultipartFile multipart = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", big);

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);

            assertThatThrownBy(() -> service.upload(10L, multipart, "student@test.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("volumineux");
        }

        @Test
        void forbidden_when_report_not_editable() {
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doThrow(new ForbiddenException("Non modifiable"))
                    .when(permissionService).assertCanEdit(draftReport, student);

            MockMultipartFile multipart = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "data".getBytes());

            assertThatThrownBy(() -> service.upload(10L, multipart, "student@test.com"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── Liste ─────────────────────────────────────────────────────────────────

    @Nested
    class ListFiles {

        @Test
        void returns_files_for_authorized_user() {
            ReportFile f = ReportFile.builder()
                    .id(1L).report(draftReport).uploadedBy(student)
                    .originalFilename("doc.pdf").storedFilename("uuid.pdf")
                    .storagePath("/tmp/uuid.pdf").contentType("application/pdf")
                    .size(100L).createdAt(LocalDateTime.now()).build();

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(reportRepository.findById(10L)).thenReturn(Optional.of(draftReport));
            doNothing().when(permissionService).assertCanRead(draftReport, student);
            when(fileRepository.findByReportIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(f));

            List<ReportFileResponseDto> result = service.listFiles(10L, "student@test.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).originalFilename()).isEqualTo("doc.pdf");
        }
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    @Nested
    class DeleteFile {

        @Test
        void deletes_record_and_disk_file() {
            Path storedPath = tempDir.resolve("10/uuid.pdf");

            ReportFile f = ReportFile.builder()
                    .id(1L).report(draftReport).uploadedBy(student)
                    .originalFilename("doc.pdf").storedFilename("uuid.pdf")
                    .storagePath(storedPath.toString()).contentType("application/pdf")
                    .size(100L).createdAt(LocalDateTime.now()).build();

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
            doNothing().when(permissionService).assertCanEdit(draftReport, student);

            service.deleteFile(1L, "student@test.com");

            verify(fileRepository).delete(f);
        }

        @Test
        void throws_when_file_not_found() {
            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(fileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteFile(99L, "student@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── Téléchargement ────────────────────────────────────────────────────────

    @Nested
    class Download {

        @Test
        void returns_resource_for_authorized_user() throws Exception {
            Path storedPath = tempDir.resolve("doc.pdf");
            java.nio.file.Files.write(storedPath, "content".getBytes());

            ReportFile f = ReportFile.builder()
                    .id(1L).report(draftReport).uploadedBy(student)
                    .originalFilename("rapport.pdf").storedFilename("doc.pdf")
                    .storagePath(storedPath.toString()).contentType("application/pdf")
                    .size(7L).createdAt(LocalDateTime.now()).build();

            when(userService.requireUser("student@test.com")).thenReturn(student);
            when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
            doNothing().when(permissionService).assertCanRead(draftReport, student);

            ResponseEntity<Resource> response = service.download(1L, "student@test.com");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentDisposition().getFilename())
                    .isEqualTo("rapport.pdf");
        }
    }
}
