package app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileStorageProperties {

    private String uploadPath = "./uploads";
    private long maxSize = 10_485_760L;
    private List<String> allowedExtensions = List.of("pdf", "docx", "txt");
    private List<String> allowedContentTypes = List.of("application/pdf", "text/plain");
}
