package com.gateprep.api.content;

import com.gateprep.api.content.ContentDtos.CatalogDto;
import com.gateprep.api.content.ContentDtos.MockCatalogDto;
import com.gateprep.api.content.ContentDtos.QuestionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService content;

    public ContentController(ContentService content) {
        this.content = content;
    }

    /** Public: subject and module names, so the home screen works before login. No questions. */
    @GetMapping("/catalog")
    public CatalogDto catalog() {
        return content.catalog();
    }

    /** Public: the list of mock tests (title, marks, time) for the Mock Test tab. No questions. */
    @GetMapping("/catalog/mocks")
    public MockCatalogDto mocks() {
        return content.mocks();
    }

    /** Needs a signed-in user. */
    @GetMapping("/modules/{moduleId}/questions")
    public List<QuestionDto> moduleQuestions(@PathVariable String moduleId) {
        return content.moduleQuestions(moduleId);
    }
}
