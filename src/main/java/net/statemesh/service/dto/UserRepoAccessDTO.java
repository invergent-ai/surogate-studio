package net.statemesh.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UserRepoAccessDTO implements Serializable {

    private String username;
}
