package net.statemesh.web.rest.k8s;

import net.statemesh.service.k8s.FileService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static net.statemesh.config.Constants.TEMP_UPLOAD_PATH;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping("/api/files")
public class FileResource {
    private final Logger log = LoggerFactory.getLogger(FileResource.class);
    private final FileService fileService;

    public FileResource(FileService fileService) {
        this.fileService = fileService;
    }

    @RequestMapping(method = POST, value = "/upload", produces = "application/json")
    public ResponseEntity<Void> uploadFile(
        @RequestParam("applicationId") final String applicationId,
        @RequestParam("podName") final String podName,
        @RequestParam(value = "containerId", required = false) final String containerId,
        @RequestParam("path") final String path,
        @RequestParam(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to upload a file for application {} in container {}", applicationId, containerId);
        if (file == null || StringUtils.isEmpty(file.getOriginalFilename())) {
            throw new RuntimeException("File was not present");
        }

        Path destinationFile = Paths.get(TEMP_UPLOAD_PATH)
            .resolve(
                Paths.get(file.getOriginalFilename())
            )
            .normalize()
            .toAbsolutePath();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            if (!fileService.uploadFile(applicationId, podName, containerId, destinationFile.toString(),
                path + File.separator + file.getOriginalFilename())) {
                throw new RuntimeException("File could not be uploaded");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Files.delete(destinationFile);
            } catch (IOException e) {
                log.error("Error deleting file: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(null);
    }

    @RequestMapping(method = GET, value = "/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("applicationId") final String applicationId,
                                                 @RequestParam("podName") final String podName,
                                                 @RequestParam(value = "containerId", required = false) final String containerId,
                                                 @RequestParam("path") final String path) {
        InputStream stream = this.fileService.downloadFile(applicationId, podName, containerId, path);
        final String fileName = path.split("/")[path.split("/").length - 1];
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(new InputStreamResource(stream));
    }
}
