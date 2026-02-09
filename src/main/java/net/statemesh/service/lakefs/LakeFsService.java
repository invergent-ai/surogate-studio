package net.statemesh.service.lakefs;

import io.lakefs.clients.sdk.*;
import io.lakefs.clients.sdk.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.repository.UserRepository;
import net.statemesh.security.JwtService;
import net.statemesh.service.dto.CreateLakeFsRepository;
import net.statemesh.service.dto.DirectLakeFsServiceParamsDTO;
import net.statemesh.service.dto.RefDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LakeFsService {
    private final ApplicationProperties properties;
    private final JwtService jwtService;
    public ApiClient client;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        client = new ApiClient();
        client.setVerifyingSsl(false);

        client.setBasePath(endpoint() + "/api/v1");
        client.setUsername(properties.getLakeFs().getKey());
        client.setPassword(properties.getLakeFs().getSecret());
    }

    public List<Repository> listRepositories() {
        String userLogin = getCurrentUsername();
        Optional<net.statemesh.domain.User> user = userRepository.findOneByLoginIgnoreCase(userLogin);
        if (user.isEmpty()) {
            throw new LakeFsException("User could not be found");
        }
        ApiClient clientForUserLogin = createClientForUser(user.get());
        var api = new RepositoriesApi(clientForUserLogin);
        try {
            var request = api.listRepositories();
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result;
        } catch (ApiException e) {
            log.error("Failed to list repositories", e);
            throw new LakeFsException("Failed to list repositories", e);
        }
    }

    public Repository createModelRepository(String repoId, String externalId) {
        CreateLakeFsRepository body = new CreateLakeFsRepository();
        body.setId(repoId);
        body.setDefaultBranch("main");

        var metadata = new HashMap<String, String>();
        metadata.put("type", LakeFsRepositoryType.MODEL.getType());
        metadata.put("displayName", body.getId());
        if (!StringUtils.isEmpty(externalId)) {
            metadata.put("externalId", externalId);
        }
        body.setMetadata(metadata);
        return createRepository(body);
    }

    public Repository createDatasetRepository(String repoId, String externalId) {
        CreateLakeFsRepository body = new CreateLakeFsRepository();
        body.setId(repoId);
        body.setDefaultBranch("main");

        var metadata = new HashMap<String, String>();
        metadata.put("type", LakeFsRepositoryType.DATASET.getType());

        if (!StringUtils.isEmpty(repoId)) {
            metadata.put("displayName", repoId);
        }
        if (!StringUtils.isEmpty(externalId)) {
            metadata.put("externalId", externalId);
        }
        body.setMetadata(metadata);
        return createRepository(body);
    }

    @Retryable(retryFor = {LakeFsNoChangesException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Repository createRepository(CreateLakeFsRepository body) {
        var username = getCurrentUsername();
        final String repoId = generateRepoId(username, body.getId());
        var api = new RepositoriesApi(client);
        Repository repository = null;
        var createRepo = new RepositoryCreation()
            .name(repoId)
            .storageNamespace("local://" + repoId)
            .defaultBranch(body.getDefaultBranch())
            .metadata(body.getMetadata());

        try {
            repository = api.createRepository(createRepo).execute();
        } catch (ApiException e) {
            if (!StringUtils.isEmpty(e.getMessage()) && e.getMessage().contains("not unique")) {
                throw new LakeFsNotUniqueException();
            }
            log.error("Failed to create {} repository {}: {}", body.getMetadata().get("type"), repoId, e.getMessage());
            throw new LakeFsException("Failed to create repository", e);
        }
        var authApi = new AuthApi(client);
        // Only continue if repo creation succeeded

        final String groupId = "repo-" + repoId;
        final String policyId = repoId + "-full-access";

        try {
            // Create or ensure the repo group exists
            try {
                authApi.getGroup(groupId).execute();
                log.debug("Group '{}' already exists", groupId);
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    authApi.createGroup()
                        .groupCreation(new GroupCreation().id(groupId))
                        .execute();
                    log.info("Group '{}' created", groupId);
                } else {
                    throw e;
                }
            }

            // Create or ensure the policy exists
            try {
                authApi.getPolicy(policyId).execute();
                log.debug("Policy '{}' already exists", policyId);
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    Policy policy = new Policy();
                    policy.setId(policyId);

                    Statement s1 = new Statement();
                    s1.setEffect(Statement.EffectEnum.ALLOW);
                    s1.setAction(List.of("fs:*"));
                    s1.setResource("arn:lakefs:fs:::repository/" + repoId);

                    Statement s2 = new Statement();
                    s2.setEffect(Statement.EffectEnum.ALLOW);
                    s2.setAction(List.of("fs:*"));
                    s2.setResource("arn:lakefs:fs:::repository/" + repoId + "/*");

                    policy.setStatement(List.of(s1, s2));

                    authApi.createPolicy(policy)
                        .execute();
                    log.info("Policy '{}' created", policyId);
                } else {
                    throw e;
                }
            }

            // Attach policy to the group
            try {
                authApi.attachPolicyToGroup(groupId, policyId).execute();
                log.info("Policy '{}' attached to group '{}'", policyId, groupId);
            } catch (ApiException e) {
                if (e.getCode() != 409) { // 409 = already attached
                    throw e;
                }
            }

            // Add repo creator to the group
            authApi.addGroupMembership(groupId, username).execute();
            log.info("User '{}' added to group '{}'", username, groupId);

        } catch (ApiException e) {
            log.error("Failed to configure permissions for repo '{}' and user '{}': {}",
                repoId, username, e.getMessage());
            // Repo exists, but permissions setup failed → you might decide to rollback or log only
            throw new LakeFsException("Repository created but permission setup failed", e);
        }

        log.debug("createRepository() - permissions configured for repo '{}'", repoId);
        return repository;
    }

    public Repository getRepository(String repoId) {
        var api = new RepositoriesApi(client);

        try {
            return api.getRepository(repoId).execute();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            log.error("Failed to get repository {}: {}", repoId, e.getMessage());
            throw new LakeFsException("Failed to get repository", e);
        }
    }

    public void deleteRepository(String id) {
        var api = new RepositoriesApi(client);
        try {
            var request = api.deleteRepository(id);
            request.force(true);
            request.execute();
            deletePolicy(id + "-full-access");
            deleteGroup("repo-" + id);
        } catch (ApiException e) {
            log.error("Failed to delete repository {}: {}", id, e.getMessage());
            throw new LakeFsException("Failed to delete repository", e);
        }
    }

    public void deletePolicy(String policyId) {
        var authApi = new AuthApi(client);
        try {
            authApi.deletePolicy(policyId).execute();
            log.info("Policy '{}' deleted", policyId);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("Policy {} not found; skipping deletion", policyId);
            } else {
                log.error("Failed to delete policy {}: {}", policyId, e.getMessage());
            }
        }
    }

    public void deleteGroup(String groupId) {
        var authApi = new AuthApi(client);
        try {
            authApi.deleteGroup(groupId).execute();
            log.info("Group '{}' deleted", groupId);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("Group {} not found; skipping deletion", groupId);
            } else {
                log.error("Failed to delete group {}: {}", groupId, e.getMessage());
            }
        }
    }

    public List<RefDTO> getBranches(String repository) {
        var api = new BranchesApi(client);
        try {
            var request = api.listBranches(repository);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result.stream().map(branch -> RefDTO.builder()
                .id(branch.getId())
                .commitId(branch.getCommitId())
                .metadata(
                    StringUtils.isEmpty(branch.getCommitId()) ? null :
                        getCommit(repository, branch.getCommitId()).getMetadata()
                )
                .build()).toList();
        } catch (ApiException e) {
            log.error("Failed to list branches for repository {}", repository, e);
            throw new LakeFsException("Failed to list branches", e);
        }
    }

    public List<Ref> getTags(String repository) {
        var api = new TagsApi(client);
        try {
            var request = api.listTags(repository);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result;
        } catch (ApiException e) {
            log.error("Failed to list tags for repository {}", repository, e);
            throw new LakeFsException("Failed to list tags", e);
        }
    }

    public List<ObjectStats> getObjects(String repository, String ref) {
        var api = new ObjectsApi(client);
        try {
            var request = api.listObjects(repository, ref);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result;
        } catch (ApiException e) {
            log.error("Failed to list objects for repository {} and ref {}", repository, ref, e);
            throw new LakeFsException("Failed to list objects", e);
        }
    }

    @Retryable(retryFor = {LakeFsNoChangesException.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void commit(String repository, String ref, CommitCreation commit) {
        var api = new CommitsApi(client);
        try {
            api.commit(repository, ref, commit).execute();
        } catch (ApiException e) {
            if (e.getMessage().contains("no changes")) {
                throw new LakeFsNoChangesException();
            }
            log.error("Failed commit for repository {} and ref {}", repository, ref, e);
            throw new LakeFsException("Failed to list objects", e);
        }
    }

    public void deleteObject(String repository, String branch, String path) {
        var api = new ObjectsApi(client);
        try {
            api.deleteObject(repository, branch, path).execute();
        } catch (ApiException e) {
            if (e.getMessage().contains("no changes")) {
                throw new LakeFsNoChangesException();
            }
            log.error("Failed to delete object {} for repository {} and branch {}", path, repository, branch, e);
            throw new LakeFsException("Failed to delete object", e);
        }
    }

    public List<Commit> getCommits(String repository, String ref) {
        var api = new RefsApi(client);
        try {
            var request = api.logCommits(repository, ref);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result;
        } catch (ApiException e) {
            log.error("Failed to list objects for repository {} and ref {}", repository, ref, e);
            throw new LakeFsException("Failed to list objects", e);
        }
    }

    public Commit getCommit(String repository, String commitId) {
        var api = new CommitsApi(client);
        try {
            return api.getCommit(repository, commitId).execute();
        } catch (ApiException e) {
            log.error("Failed to get commit repository {} and id {}", repository, repository, e);
            throw new LakeFsException("Failed get commit", e);
        }
    }

    public List<Diff> getDiff(String repository, String leftRef, String rightRef) {
        var api = new RefsApi(client);
        try {
            var request = api.diffRefs(repository, leftRef, rightRef);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());

            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }

            return result;
        } catch (ApiException e) {
            log.error("Failed to get diff for repository {} and refs {}, {}", repository, leftRef, rightRef, e);
            throw new LakeFsException("Failed to get diff", e);
        }
    }

    public ObjectStats getStat(String repository, String ref, String path) {
        var api = new ObjectsApi(client);
        try {
            return api.statObject(repository, ref, path).execute();
        } catch (ApiException e) {
            log.error("Failed to get stat: repository {} and ref {} and path {}", repository, ref, path, e);
            throw new LakeFsException("Failed get stat", e);
        }
    }

    public void createBranch(String repository, BranchCreation branch) {
        var api = new BranchesApi(client);
        try {
            api.createBranch(repository, branch).execute();
        } catch (ApiException e) {
            log.error("Failed create branch for repository {} and branch {}", repository, branch, e);
            throw new LakeFsException("Failed to create branch", e);
        }
    }

    public void deleteBranch(String repository, String branch) {
        var api = new BranchesApi(client);
        try {
            api.deleteBranch(repository, branch).execute();
        } catch (ApiException e) {
            log.error("Failed delete branch for repository {} and branch {}", repository, branch, e);
            throw new LakeFsException("Failed to delete branch", e);
        }
    }

    public void createTag(String repository, TagCreation tag) {
        var api = new TagsApi(client);
        try {
            api.createTag(repository, tag).execute();
        } catch (ApiException e) {
            log.error("Failed create tag for repository {} and tag {}", repository, tag, e);
            throw new LakeFsException("Failed to create tag", e);
        }
    }

    public void deleteTag(String repository, String tag) {
        var api = new TagsApi(client);
        try {
            api.deleteTag(repository, tag).execute();
        } catch (ApiException e) {
            log.error("Failed delete tag for repository {} and tag {}", repository, tag, e);
            throw new LakeFsException("Failed to delete tag", e);
        }
    }

    public DirectLakeFsServiceParamsDTO getDirectServiceParams() {
        var auth = Base64.getEncoder().encodeToString(
            String.format("%s:%s",
                properties.getLakeFs().getKey(), properties.getLakeFs().getSecret()).getBytes()
        );
        var s3Auth = jwtService.createLakeFsToken();
        var s3Endpoint = properties.getLakeFs().getS3Endpoint();
        return new DirectLakeFsServiceParamsDTO(properties.getLakeFs().getEndpoint(), auth, s3Auth, s3Endpoint);
    }

    public CredentialsWithSecret createUser(String username) {
        var api = new AuthApi(client);
        var user = new UserCreation();
        user.setId(username);
        user.setInviteUser(false);

        final String USERS_GROUP = "Users";
        final String USERS_POLICY = "users-policy";

        try {
            // Create user
            api.createUser().userCreation(user).execute();

            // Create credentials for that user
            CredentialsWithSecret creds = api.createCredentials(username).execute();

            // Ensure "Users" group exists
            try {
                api.getGroup(USERS_GROUP).execute();
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    api.createGroup().groupCreation(new GroupCreation().id(USERS_GROUP)).execute();
                    log.info("Group '{}' created", USERS_GROUP);
                } else {
                    throw e;
                }
            }

            // Ensure "users-policy" exists
            try {
                api.getPolicy(USERS_POLICY).execute();
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    Policy usersPolicy = new Policy();
                    usersPolicy.setId(USERS_POLICY);

                    Statement s1 = new Statement();
                    s1.setEffect(Statement.EffectEnum.ALLOW);
                    s1.setAction(List.of(
                        "fs:ListRepositories",
                        "fs:CreateRepository",
                        "fs:AttachStorageNamespace"
                    ));
                    s1.setResource("*");

                    Statement s2 = new Statement();
                    s2.setEffect(Statement.EffectEnum.ALLOW);
                    s2.setAction(List.of(
                        "auth:ListPolicies",
                        "auth:GetPolicy",
                        "auth:CreatePolicy",
                        "auth:UpdatePolicy",
                        "auth:AttachPolicyToUser",
                        "auth:AttachPolicy",
                        "auth:ListPolicies"
                    ));
                    s2.setResource("*");

                    usersPolicy.setStatement(List.of(s1, s2));
                    api.createPolicy(usersPolicy).execute();
                    log.info("Policy '{}' created", USERS_POLICY);
                } else {
                    throw e;
                }
            }

            // Attach "users-policy" to "Users" group
            try {
                api.attachPolicyToGroup(USERS_GROUP, USERS_POLICY).execute();
                log.info("Policy '{}' attached to group '{}'", USERS_POLICY, USERS_GROUP);
            } catch (ApiException e) {
                // 409 means it's already attached
                if (e.getCode() != 409) {
                    throw e;
                }
            }

            // Add user to "Users" group
            api.addGroupMembership(USERS_GROUP, username).execute();
            log.debug("User '{}' added to group '{}'", username, USERS_GROUP);

            return creds;

        } catch (ApiException e) {
            log.error("Failed to create user {}", username, e);
            throw new LakeFsException("Failed to create user", e);
        }
    }

    public void addUserToRepoGroup(String repoId, String username) {
        var authApi = new AuthApi(client);
        final String groupId = "repo-" + repoId;

        try {
            // Ensure the group exists
            try {
                authApi.getGroup(groupId).execute();
                log.debug("Group '{}' exists", groupId);
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    log.warn("Group '{}' not found when adding user '{}'", groupId, username);
                    throw new LakeFsException("Repository group not found: " + groupId, e);
                } else {
                    throw e;
                }
            }

            //  Add user to the group
            try {
                authApi.addGroupMembership(
                        groupId, username)
                    .execute();
                log.info("User '{}' added to group '{}'", username, groupId);
            } catch (ApiException e) {
                if (e.getCode() == 409) {
                    log.debug("User '{}' already a member of group '{}'", username, groupId);
                } else {
                    throw e;
                }
            }

        } catch (ApiException e) {
            log.error("Failed to add user '{}' to group '{}': {}", username, groupId, e.getMessage());
            throw new LakeFsException("Failed to add user to repository group", e);
        }
    }

    public List<User> listGroupMembers(String groupId) {
        var api = new AuthApi(client);
        try {
            var request = api.listGroupMembers(groupId);
            var list = request.execute();
            var result = new ArrayList<>(list.getResults());
            while (list.getPagination().getHasMore()) {
                request.after(list.getPagination().getNextOffset());
                list = request.execute();
                result.addAll(list.getResults());
            }
            return result;
        } catch (ApiException e) {
            log.error("Failed to list group members", e);
            throw new LakeFsException("Failed to list group members", e);
        }
    }

    public void deleteGroupMember(String groupId, String username) {
        var api = new AuthApi(client);
        try {
            var request = api.deleteGroupMembership(groupId, username);
            request.execute();
        } catch (ApiException e) {
            log.error("Failed to delete group member: {}, username: {}, : {}", groupId, username, e.getMessage());
            throw new LakeFsException("Failed to delete repository", e);
        }
    }

    private String generateRepoId(String email, String repoName) {
        final int MAX_ID_LENGTH = 63;
        String sanitizedUser = email
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        int maxRepoLength = MAX_ID_LENGTH - sanitizedUser.length() - 1;
        String sanitizedRepo = repoName
            .toLowerCase()
            .replaceAll("[^a-z0-9-]", "-");
        if (sanitizedRepo.length() > maxRepoLength) {
            throw new LakeFsException(
                String.format("Repository name too long. Maximum allowed length is %d characters.",
                    maxRepoLength)
            );
        }
        return sanitizedUser + "-" + sanitizedRepo;
    }

    private ApiClient createClientForUser(net.statemesh.domain.User user) {
        if (StringUtils.isEmpty(user.getLakeFsAccessKey())) {
            return client;
        }
        ApiClient currentUserApiClient = new ApiClient();
        currentUserApiClient.setVerifyingSsl(false);
        currentUserApiClient.setBasePath(endpoint() + "/api/v1");
        currentUserApiClient.setUsername(user.getLakeFsAccessKey());
        currentUserApiClient.setPassword(user.getLakeFsSecretKey());

        return currentUserApiClient;
    }


    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LakeFsException("No authenticated user found");
        }
        return authentication.getName();
    }

    public byte[] getObjectContent(String repository, String ref, String path) {
        var api = new ObjectsApi(client);
        try {
            File file = api.getObject(repository, ref, path).execute();
            return java.nio.file.Files.readAllBytes(file.toPath());
        } catch (ApiException e) {
            log.error("Failed to get object content: repository {} ref {} path {}", repository, ref, path, e);
            throw new LakeFsException("Failed to get object content", e);
        } catch (java.io.IOException e) {
            log.error("Failed to read object content: repository {} ref {} path {}", repository, ref, path, e);
            throw new LakeFsException("Failed to read object content", e);
        }
    }

    public void uploadObject(String repository, String branch, String path, byte[] content) {
        var api = new ObjectsApi(client);
        try {
            File tempFile = File.createTempFile("lakefs-upload", ".tmp");
            java.nio.file.Files.write(tempFile.toPath(), content);
            api.uploadObject(repository, branch, path).content(tempFile).execute();
            tempFile.delete();
        } catch (ApiException e) {
            log.error("Failed to upload object: repository {} branch {} path {}", repository, branch, path, e);
            throw new LakeFsException("Failed to upload object", e);
        } catch (java.io.IOException e) {
            log.error("Failed to write temp file for upload", e);
            throw new LakeFsException("Failed to upload object", e);
        }
    }

    private String endpoint() {
        return Optional.ofNullable(properties.getK8sAccessMode()).orElse(Boolean.FALSE) ?
            properties.getLakeFs().getEndpointInternal() : properties.getLakeFs().getEndpoint();
    }
}
