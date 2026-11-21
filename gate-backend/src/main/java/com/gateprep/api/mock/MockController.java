package com.gateprep.api.mock;

import com.gateprep.api.content.ContentDtos.QuestionDto;
import com.gateprep.api.content.ContentService;
import com.gateprep.api.mock.MockService.Leaderboard;
import com.gateprep.api.mock.MockService.SaveRequest;
import com.gateprep.api.mock.MockService.SessionDto;
import com.gateprep.api.mock.MockService.SubmitRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mocks/{mockId}")
public class MockController {

    private final MockService mocks;
    private final ContentService content;

    public MockController(MockService mocks, ContentService content) {
        this.mocks = mocks;
        this.content = content;
    }

    private static UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** The 65 questions, without answers. */
    @GetMapping("/questions")
    public List<QuestionDto> questions(@PathVariable String mockId) {
        return content.mockQuestions(mockId);
    }

    /** Latest sitting of this user (204 if none yet). */
    @GetMapping("/session")
    public ResponseEntity<SessionDto> session(@AuthenticationPrincipal Jwt jwt, @PathVariable String mockId) {
        return mocks.current(user(jwt), mockId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Starts a sitting (or returns the one that is already running). restart=true begins a new one. */
    @PostMapping("/start")
    public SessionDto start(@AuthenticationPrincipal Jwt jwt, @PathVariable String mockId, @RequestParam(defaultValue = "false") boolean restart) {
        return mocks.start(user(jwt), mockId, restart);
    }

    /** Auto-save of answers, marked-for-review and position while the test runs. */
    @PutMapping("/session")
    public ResponseEntity<Void> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String mockId, @RequestBody SaveRequest r) {
        mocks.save(user(jwt), mockId, r);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit")
    public SessionDto submit(@AuthenticationPrincipal Jwt jwt, @PathVariable String mockId, @RequestBody(required = false) SubmitRequest r) {
        return mocks.submit(user(jwt), mockId, r == null ? null : r.answers());
    }

    @GetMapping("/leaderboard")
    public Leaderboard leaderboard(@AuthenticationPrincipal Jwt jwt, @PathVariable String mockId) {
        return mocks.leaderboard(user(jwt), mockId);
    }
}
