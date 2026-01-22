package net.statemesh.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportLakeFsJob {
    String source;
    String repo;
    String branch;
    String token;
}
