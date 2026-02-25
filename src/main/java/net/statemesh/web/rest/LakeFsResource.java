package net.statemesh.web.rest;

import io.lakefs.clients.sdk.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.dto.*;
import net.statemesh.service.lakefs.LakeFsException;
import net.statemesh.service.lakefs.LakeFsNoChangesException;
import net.statemesh.service.lakefs.LakeFsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/api/lakefs")
@RequiredArgsConstructor
@Tag(name = "LakeFS", description = "LakeFS data versioning and repository management")
public class LakeFsResource {
    private final LakeFsService lakeFsService;

    @Operation(summary = "Get direct service params")
    @ApiResponse(responseCode = "200", description = "Service params returned")
    @GetMapping("/config")
    public ResponseEntity<DirectLakeFsServiceParamsDTO> getDirectServiceParams() {
        return ResponseEntity.ok(this.lakeFsService.getDirectServiceParams());
    }

    @Operation(summary = "List repositories")
    @ApiResponse(responseCode = "200", description = "Repositories returned")
    @ApiResponse(responseCode = "400", description = "Error listing repositories")
    @GetMapping("/repos")
    public ResponseEntity<List<Repository>> listRepositories() {
        try {
            return ResponseEntity.ok(lakeFsService.listRepositories());
        } catch (LakeFsException ex) {
            log.error("Problem listing repositories", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "List branches for a repository")
    @ApiResponse(responseCode = "200", description = "Branches returned")
    @GetMapping("/branches/{repoId}")
    public ResponseEntity<List<RefDTO>> listBranches(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId) {
        try {
            return ResponseEntity.ok(lakeFsService.getBranches(repoId));
        } catch (LakeFsException ex) {
            log.error("Problem listing branches", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Create a branch")
    @ApiResponse(responseCode = "200", description = "Branch created")
    @PostMapping("/branches/{repoId}")
    public ResponseEntity<Void> createBranch(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @RequestBody BranchCreation branch) {
        try {
            lakeFsService.createBranch(repoId, branch);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem creating branch", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Delete a branch")
    @ApiResponse(responseCode = "200", description = "Branch deleted")
    @DeleteMapping("/branches/{repoId}/{ref}")
    public ResponseEntity<Void> deleteBranch(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Branch ref") @PathVariable("ref") String ref) {
        try {
            lakeFsService.deleteBranch(repoId, ref);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting branch", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "List tags for a repository")
    @ApiResponse(responseCode = "200", description = "Tags returned")
    @GetMapping("/tags/{repoId}")
    public ResponseEntity<List<Ref>> listTags(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId) {
        try {
            return ResponseEntity.ok(lakeFsService.getTags(repoId));
        } catch (LakeFsException ex) {
            log.error("Problem listing tags", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Create a tag")
    @ApiResponse(responseCode = "200", description = "Tag created")
    @PostMapping("/tags/{repoId}")
    public ResponseEntity<Void> createTag(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @RequestBody TagCreation tag) {
        try {
            lakeFsService.createTag(repoId, tag);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem creating tag", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Delete a tag")
    @ApiResponse(responseCode = "200", description = "Tag deleted")
    @DeleteMapping("/tags/{repoId}/{ref}")
    public ResponseEntity<Void> deleteTag(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Tag ref") @PathVariable("ref") String ref) {
        try {
            lakeFsService.deleteTag(repoId, ref);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting tag", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "List objects in a ref")
    @ApiResponse(responseCode = "200", description = "Objects returned")
    @GetMapping("/objects/{repoId}/{ref}")
    public ResponseEntity<List<ObjectStats>> listObjects(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Branch or tag ref") @PathVariable("ref") String ref) {
        try {
            return ResponseEntity.ok(lakeFsService.getObjects(repoId, ref));
        } catch (LakeFsException ex) {
            log.error("Problem listing objects", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Delete an object")
    @ApiResponse(responseCode = "200", description = "Object deleted")
    @DeleteMapping("/objects/{repoId}/{branch}")
    public ResponseEntity<Void> deleteObject(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Branch name") @PathVariable("branch") String branch,
        @Parameter(description = "Object path") @RequestParam("path") String path) {
        try {
            lakeFsService.deleteObject(repoId, branch, path);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting object", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Create a repository")
    @ApiResponse(responseCode = "200", description = "Repository created")
    @PostMapping("/repos")
    public ResponseEntity<Repository> createRepository(@RequestBody CreateLakeFsRepository body) {
        try {
            return ResponseEntity.ok(lakeFsService.createRepository(body));
        } catch (LakeFsException ex) {
            log.error("Problem creating repository", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Add user to repository group")
    @ApiResponse(responseCode = "200", description = "User added")
    @PostMapping("/repos/{repoId}/users")
    public ResponseEntity<Void> addUserToRepoGroup(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @RequestBody UserRepoAccessDTO body) {
        try {
            lakeFsService.addUserToRepoGroup(repoId, body.getUsername());
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem adding user '{}' to repo '{}': {}", body.getUsername(), repoId, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error adding user '{}' to repo '{}': {}", body.getUsername(), repoId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete a repository")
    @ApiResponse(responseCode = "200", description = "Repository deleted")
    @DeleteMapping("/repos/{id}")
    public ResponseEntity<Void> deleteRepository(
        @Parameter(description = "Repository ID") @PathVariable("id") String repoId) {
        try {
            lakeFsService.deleteRepository(repoId);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting repository", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Import data (placeholder)")
    @ApiResponse(responseCode = "200", description = "Import acknowledged")
    @PostMapping("/import")
    public ResponseEntity<Void> importData(@RequestBody ImportLakeFsJob body) {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Commit changes to a branch")
    @ApiResponse(responseCode = "200", description = "Changes committed")
    @PostMapping("/commit/{repoId}/{branchId}")
    public ResponseEntity<Void> commit(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Branch ID") @PathVariable("branchId") String branchId,
        @RequestBody CommitCreation body) {
        try {
            lakeFsService.commit(repoId, branchId, body);
            return ResponseEntity.ok().build();
        } catch (LakeFsNoChangesException ex) {
            log.debug("No changes to commit for repo {} branch {}", repoId, branchId);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem committing", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get a commit by ID")
    @ApiResponse(responseCode = "200", description = "Commit returned")
    @GetMapping("/commit/{repoId}/{commitId}")
    public ResponseEntity<Commit> getCommit(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Commit ID") @PathVariable("commitId") String commitId) {
        try {
            return ResponseEntity.ok(lakeFsService.getCommit(repoId, commitId));
        } catch (LakeFsException ex) {
            log.error("Problem getting commit", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Get object stats")
    @ApiResponse(responseCode = "200", description = "Stats returned")
    @GetMapping("/stat/{repoId}/{refId}")
    public ResponseEntity<ObjectStats> getStat(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Ref ID") @PathVariable("refId") String refId,
        @Parameter(description = "Object path") @RequestParam("path") String path) {
        try {
            return ResponseEntity.ok(lakeFsService.getStat(repoId, refId, path));
        } catch (LakeFsException ex) {
            log.error("Problem getting stat", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Get diff between two refs")
    @ApiResponse(responseCode = "200", description = "Diff returned")
    @GetMapping("/diff/{repoId}/{leftRef}/{rightRef}")
    public ResponseEntity<List<Diff>> getDiff(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Left ref") @PathVariable("leftRef") String leftRef,
        @Parameter(description = "Right ref") @PathVariable("rightRef") String rightRef) {
        try {
            return ResponseEntity.ok(lakeFsService.getDiff(repoId, leftRef, rightRef));
        } catch (LakeFsException ex) {
            log.error("Problem getting diff", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Get commits for a ref")
    @ApiResponse(responseCode = "200", description = "Commits returned")
    @GetMapping("/commits/{repoId}/{refId}")
    public ResponseEntity<List<Commit>> getCommits(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Ref ID") @PathVariable("refId") String refId) {
        try {
            return ResponseEntity.ok(lakeFsService.getCommits(repoId, refId));
        } catch (LakeFsException ex) {
            log.error("Problem getting commits", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "List group members")
    @ApiResponse(responseCode = "200", description = "Members returned")
    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<List<User>> listGroupMembers(
        @Parameter(description = "Group ID") @PathVariable("groupId") String groupId) {
        try {
            return ResponseEntity.ok(lakeFsService.listGroupMembers(groupId));
        } catch (LakeFsException ex) {
            log.error("Problem listing group members", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Remove a member from a group")
    @ApiResponse(responseCode = "200", description = "Member removed")
    @DeleteMapping("/group/{groupId}/members/{username}")
    public ResponseEntity<Void> deleteGroupMembers(
        @Parameter(description = "Group ID") @PathVariable("groupId") String groupId,
        @Parameter(description = "Username") @PathVariable("username") String username) {
        try {
            lakeFsService.deleteGroupMember(groupId, username);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting group members", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "List evaluation result files")
    @ApiResponse(responseCode = "200", description = "Eval result files returned")
    @GetMapping("/eval-results/{repo}/{branch}")
    public ResponseEntity<List<String>> listEvalResults(
        @Parameter(description = "Repository name") @PathVariable("repo") String repo,
        @Parameter(description = "Branch name") @PathVariable("branch") String branch) {
        try {
            List<ObjectStats> objects = lakeFsService.getObjects(repo, branch);
            List<String> evalFiles = objects.stream()
                .map(ObjectStats::getPath)
                .filter(path -> path.startsWith("eval_results/"))
                .map(path -> path.substring("eval_results/".length()))
                .toList();
            return ResponseEntity.ok(evalFiles);
        } catch (LakeFsException ex) {
            log.error("Problem listing eval results", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get evaluation result content")
    @ApiResponse(responseCode = "200", description = "Eval result content returned")
    @ApiResponse(responseCode = "404", description = "File not found")
    @GetMapping("/eval-results/{repo}/{branch}/{filename}")
    public ResponseEntity<byte[]> getEvalResult(
        @Parameter(description = "Repository name") @PathVariable("repo") String repo,
        @Parameter(description = "Branch name") @PathVariable("branch") String branch,
        @Parameter(description = "File name") @PathVariable("filename") String filename) {
        try {
            byte[] content = lakeFsService.getObjectContent(repo, branch, "eval_results/" + filename);
            return ResponseEntity.ok(content);
        } catch (LakeFsException ex) {
            log.error("Problem getting eval result: {}", filename, ex);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get object content")
    @ApiResponse(responseCode = "200", description = "Object content returned")
    @GetMapping("/objects/{repoId}/{ref}/content")
    public ResponseEntity<byte[]> getObjectContent(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Ref") @PathVariable("ref") String ref,
        @Parameter(description = "Object path") @RequestParam("path") String path) {
        try {
            byte[] content = lakeFsService.getObjectContent(repoId, ref, path);
            return ResponseEntity.ok()
                .header("Content-Length", String.valueOf(content.length))
                .body(content);
        } catch (LakeFsException ex) {
            log.error("Problem getting object content", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Download an object")
    @ApiResponse(responseCode = "200", description = "Object downloaded")
    @GetMapping("/objects/{repoId}/{ref}/download")
    public ResponseEntity<byte[]> downloadObject(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Ref") @PathVariable("ref") String ref,
        @Parameter(description = "Object path") @RequestParam("path") String path) {
        try {
            byte[] content = lakeFsService.getObjectContent(repoId, ref, path);
            String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/octet-stream")
                .body(content);
        } catch (LakeFsException ex) {
            log.error("Problem downloading object", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Upload an object")
    @ApiResponse(responseCode = "200", description = "Object uploaded")
    @PostMapping("/objects/{repoId}/{branch}/upload")
    public ResponseEntity<Void> uploadObject(
        @Parameter(description = "Repository ID") @PathVariable("repoId") String repoId,
        @Parameter(description = "Branch name") @PathVariable("branch") String branch,
        @Parameter(description = "Object path") @RequestParam("path") String path,
        @Parameter(description = "File to upload") @RequestParam("content") MultipartFile file) {
        try {
            lakeFsService.uploadObject(repoId, branch, path, file.getBytes());
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem uploading object", ex);
            return ResponseEntity.badRequest().build();
        } catch (java.io.IOException e) {
            log.error("Problem reading uploaded file", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
