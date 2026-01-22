package net.statemesh.web.rest.s3;

import lombok.RequiredArgsConstructor;
import net.statemesh.service.s3.S3Service;
import net.statemesh.service.dto.ValidationS3ResponseDTO;
import net.statemesh.service.dto.S3CredentialsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Resource {
    private final S3Service s3Service;

    @PostMapping("/s3/validate")
    public ResponseEntity<ValidationS3ResponseDTO> validateS3Credentials(
        @RequestBody S3CredentialsDTO credentials) {
        try {
            boolean isValid = s3Service.validateCredentials(credentials);

            ValidationS3ResponseDTO response = new ValidationS3ResponseDTO();
            response.setValid(isValid);
            response.setMessage(isValid ?
                "S3 connection successful" :
                "Failed to connect to S3 server");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ValidationS3ResponseDTO response = new ValidationS3ResponseDTO();
            response.setValid(false);
            response.setMessage("Error validating S3 connection: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
