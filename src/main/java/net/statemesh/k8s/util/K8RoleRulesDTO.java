package net.statemesh.k8s.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class K8RoleRulesDTO {
    List<String> apiGroups;
    List<String> resources;
    List<String> verbs;
}
