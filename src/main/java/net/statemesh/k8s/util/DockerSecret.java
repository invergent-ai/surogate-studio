package net.statemesh.k8s.util;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DockerSecret {
    private Map<String, Auth> auths;

    @Data
    @Builder
    public static class Auth {
        private String username;
        private String password;
        private String auth;
    }
}
