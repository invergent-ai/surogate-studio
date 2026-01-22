package net.statemesh.service.dto;

import io.lakefs.clients.sdk.model.Repository;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLakeFsRepository extends Repository {
    String description;
}
