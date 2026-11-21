package com.gateprep.api.mock;

import com.gateprep.api.mock.MockService.Summary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class MockSummaryController {

    private final MockService mocks;

    public MockSummaryController(MockService mocks) {
        this.mocks = mocks;
    }

    /** Status of every mock for this user: latest sitting (without answers), best score and finished attempts. */
    @GetMapping("/api/mock-summary")
    public Map<String, Summary> summary(@AuthenticationPrincipal Jwt jwt) {
        return mocks.summaries(UUID.fromString(jwt.getSubject()));
    }
}
