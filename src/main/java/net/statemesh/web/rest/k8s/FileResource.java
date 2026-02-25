package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Files", description = "File upload and download for application containers")
public class FileResource {
    private final Logger log = LoggerFactory.getLogger(FileResource.class);
    private final FileService fileService;

    public FileResource(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Upload file to container", description = "Upload a file to a specific path in an application container")
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @RequestMapping(method = POST, value = "/upload", produces = "application/json")
    public ResponseEntity<Void> uploadFile(
        @Parameter(description = "Application ID") @RequestParam("applicationId") final String applicationId,
        @Parameter(description = "Pod name") @RequestParam("podName") final String podName,
        @Parameter(description = "Container ID") @RequestParam(value = "containerId", required = false) final String containerId,
        @Parameter(description = "Destination path in container") @RequestParam("path") final String path,
        @Parameter(description = "File to upload") @RequestParam(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to upload a file for application {} in container {}", applicationId, containerId);
        if (file == null || StringUtils.isEmpty(file.getOriginalFilename())) {
            throw new RuntimeException("File was not present");
        }

        Path destinationFile = Paths.get(TEMP_UPLOAD_PATH)
            .resolve(Paths.get(file.getOriginalFilename()))
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

    @Operation(summary = "Download file from container", description = "Download a file from a specific path in an application container")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully")
    @RequestMapping(method = GET, value = "/download")
    public ResponseEntity<Resource> downloadFile(
        @Parameter(description = "Application ID") @RequestParam("applicationId") final String applicationId,
        @Parameter(description = "Pod name") @RequestParam("podName") final String podName,
        @Parameter(description = "Container ID") @RequestParam(value = "containerId", required = false) final String containerId,
        @Parameter(description = "File path in container") @RequestParam("path") final String path) {
        InputStream stream = this.fileService.downloadFile(applicationId, podName, containerId, path);
        final String fileName = path.split("/")[path.split("/").length - 1];
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(new InputStreamResource(stream));
    }
}
