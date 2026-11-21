package com.gateprep.api.progress;

import com.gateprep.api.progress.ProgressService.AttemptRec;
import com.gateprep.api.progress.ProgressService.Progress;
import com.gateprep.api.progress.ProgressService.StateUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    public record AttemptRequest(@NotBlank String questionId, @NotBlank String selection) {}

    private final ProgressService progress;

    public ProgressController(ProgressService progress) {
        this.progress = progress;
    }

    private static UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @GetMapping
    public Progress get(@AuthenticationPrincipal Jwt jwt) {
        return progress.get(user(jwt));
    }

    @PostMapping("/attempts")
    public AttemptRec attempt(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AttemptRequest r) {
        return progress.recordAttempt(user(jwt), r.questionId(), r.selection());
    }

    @DeleteMapping("/attempts/{questionId}")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt, @PathVariable String questionId) {
        progress.clearAttempts(user(jwt), questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/viewed/{questionId}")
    public ResponseEntity<Void> viewed(@AuthenticationPrincipal Jwt jwt, @PathVariable String questionId, @RequestParam boolean value) {
        progress.setViewed(user(jwt), questionId, value);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/state")
    public ResponseEntity<Void> state(@AuthenticationPrincipal Jwt jwt, @RequestBody StateUpdate s) {
        progress.saveState(user(jwt), s);
        return ResponseEntity.noContent().build();
    }
}
