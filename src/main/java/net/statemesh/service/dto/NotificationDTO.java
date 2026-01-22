package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.statemesh.domain.enumeration.NotificationType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Notification} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
public class NotificationDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 500)
    private String message;

    private Boolean read;

    private Boolean mailSent;

    @NotNull
    private LocalDateTime createdTime;

    private NotificationType type;

    private Map<String, String> extraProperties;

    private UserDTO user;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationDTO)) {
            return false;
        }

        NotificationDTO notificationDTO = (NotificationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, notificationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
