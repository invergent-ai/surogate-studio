package net.statemesh.web.rest;

import io.lakefs.clients.sdk.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.dto.*;
import net.statemesh.service.lakefs.LakeFsException;
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
public class LakeFsResource {
    private final LakeFsService lakeFsService;

    @GetMapping("/config")
    public ResponseEntity<DirectLakeFsServiceParamsDTO> getDirectServiceParams() {
        return ResponseEntity.ok(this.lakeFsService.getDirectServiceParams());
    }

    @GetMapping("/repos")
    public ResponseEntity<List<Repository>> listRepositories() {
        try {
            return ResponseEntity.ok(lakeFsService.listRepositories());
        } catch (LakeFsException ex) {
            log.error("Problem listing repositories", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/branches/{repoId}")
    public ResponseEntity<List<RefDTO>> listBranches(@PathVariable("repoId") String repoId) {
        try {
            return ResponseEntity.ok(lakeFsService.getBranches(repoId));
        } catch (LakeFsException ex) {
            log.error("Problem listing branches", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/branches/{repoId}")
    public ResponseEntity<Void> createBranch(@PathVariable("repoId") String repoId, @RequestBody BranchCreation branch) {
        try {
            lakeFsService.createBranch(repoId, branch);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem creating branch", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/branches/{repoId}/{ref}")
    public ResponseEntity<Void> deleteBranch(@PathVariable("repoId") String repoId, @PathVariable("ref") String ref) {
        try {
            lakeFsService.deleteBranch(repoId, ref);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting branch", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/tags/{repoId}")
    public ResponseEntity<List<Ref>> listTags(@PathVariable("repoId") String repoId) {
        try {
            return ResponseEntity.ok(lakeFsService.getTags(repoId));
        } catch (LakeFsException ex) {
            log.error("Problem listing tags", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/tags/{repoId}")
    public ResponseEntity<Void> createTag(@PathVariable("repoId") String repoId, @RequestBody TagCreation tag) {
        try {
            lakeFsService.createTag(repoId, tag);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem creating tag", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/tags/{repoId}/{ref}")
    public ResponseEntity<Void> deleteTag(@PathVariable("repoId") String repoId, @PathVariable("ref") String ref) {
        try {
            lakeFsService.deleteTag(repoId, ref);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting tag", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/objects/{repoId}/{ref}")
    public ResponseEntity<List<ObjectStats>> listObjects(@PathVariable("repoId") String repoId, @PathVariable("ref") String ref) {
        try {
            return ResponseEntity.ok(lakeFsService.getObjects(repoId, ref));
        } catch (LakeFsException ex) {
            log.error("Problem listing objects", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/objects/{repoId}/{branch}")
    public ResponseEntity<Void> deleteObject(@PathVariable("repoId") String repoId,
                                             @PathVariable("branch") String branch,
                                             @RequestParam("path") String path) {
        try {
            lakeFsService.deleteObject(repoId, branch, path);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting object", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/repos")
    public ResponseEntity<Repository> createRepository(@RequestBody CreateLakeFsRepository body) {
        try {
            return ResponseEntity.ok(lakeFsService.createRepository(body));
        } catch (LakeFsException ex) {
            log.error("Problem creating repository", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/repos/{repoId}/users")
    public ResponseEntity<Void> addUserToRepoGroup(
        @PathVariable("repoId") String repoId,
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

    @DeleteMapping("/repos/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable("id") String repoId) {
        try {
            lakeFsService.deleteRepository(repoId);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting repository", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importData(@RequestBody ImportLakeFsJob body) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/commit/{repoId}/{branchId}")
    public ResponseEntity<Void> commit(@PathVariable("repoId") String repoId, @PathVariable("branchId") String branchId, @RequestBody CommitCreation body) {
        try {
            lakeFsService.commit(repoId, branchId, body);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting repository", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/commit/{repoId}/{commitId}")
    public ResponseEntity<Commit> getCommit(@PathVariable("repoId") String repoId, @PathVariable("commitId") String commitId) {
        try {
            return ResponseEntity.ok(lakeFsService.getCommit(repoId, commitId));
        } catch (LakeFsException ex) {
            log.error("Problem getting commit", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/stat/{repoId}/{refId}")
    public ResponseEntity<ObjectStats> getStat(
        @PathVariable("repoId") String repoId,
        @PathVariable("refId") String refId,
        @RequestParam("path") String path) {
        try {
            return ResponseEntity.ok(lakeFsService.getStat(repoId, refId, path));
        } catch (LakeFsException ex) {
            log.error("Problem getting stat", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/diff/{repoId}/{leftRef}/{rightRef}")
    public ResponseEntity<List<Diff>> getDiff(@PathVariable("repoId") String repoId, @PathVariable("leftRef") String leftRef, @PathVariable("rightRef") String rightRef) {
        try {
            return ResponseEntity.ok(lakeFsService.getDiff(repoId, leftRef, rightRef));
        } catch (LakeFsException ex) {
            log.error("Problem getting diff", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/commits/{repoId}/{refId}")
    public ResponseEntity<List<Commit>> getCommits(@PathVariable("repoId") String repoId, @PathVariable("refId") String refId) {
        try {
            return ResponseEntity.ok(lakeFsService.getCommits(repoId, refId));
        } catch (LakeFsException ex) {
            log.error("Problem getting commits", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<List<User>> listGroupMembers(@PathVariable("groupId") String groupId) {
        try {
            return ResponseEntity.ok(lakeFsService.listGroupMembers(groupId));
        } catch (LakeFsException ex) {
            log.error("Problem listing group members", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/group/{groupId}/members/{username}")
    public ResponseEntity<Void> deleteGroupMembers(@PathVariable("groupId") String groupId, @PathVariable("username") String username) {
        try {
            lakeFsService.deleteGroupMember(groupId, username);
            return ResponseEntity.ok().build();
        } catch (LakeFsException ex) {
            log.error("Problem deleting group members", ex);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/eval-results/{repo}/{branch}")
    public ResponseEntity<List<String>> listEvalResults(
        @PathVariable("repo") String repo,
        @PathVariable("branch") String branch) {
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

    @GetMapping("/eval-results/{repo}/{branch}/{filename}")
    public ResponseEntity<byte[]> getEvalResult(
        @PathVariable("repo") String repo,
        @PathVariable("branch") String branch,
        @PathVariable("filename") String filename) {
        try {
            byte[] content = lakeFsService.getObjectContent(repo, branch, "eval_results/" + filename);
            return ResponseEntity.ok(content);
        } catch (LakeFsException ex) {
            log.error("Problem getting eval result: {}", filename, ex);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/objects/{repoId}/{ref}/content")
    public ResponseEntity<byte[]> getObjectContent(
        @PathVariable("repoId") String repoId,
        @PathVariable("ref") String ref,
        @RequestParam("path") String path) {
        try {
            byte[] content = lakeFsService.getObjectContent(repoId, ref, path);
            return ResponseEntity.ok(content);
        } catch (LakeFsException ex) {
            log.error("Problem getting object content", ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/objects/{repoId}/{ref}/download")
    public ResponseEntity<byte[]> downloadObject(
        @PathVariable("repoId") String repoId,
        @PathVariable("ref") String ref,
        @RequestParam("path") String path) {
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

    @PostMapping("/objects/{repoId}/{branch}/upload")
    public ResponseEntity<Void> uploadObject(
        @PathVariable("repoId") String repoId,
        @PathVariable("branch") String branch,
        @RequestParam("path") String path,
        @RequestParam("content") MultipartFile file) {
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
