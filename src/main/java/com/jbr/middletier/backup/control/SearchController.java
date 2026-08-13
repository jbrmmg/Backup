package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.SearchRequestDTO;
import com.jbr.middletier.backup.dto.SearchResponseDTO;
import com.jbr.middletier.backup.exception.InvalidSearchRequestException;
import com.jbr.middletier.backup.manager.SearchManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Search", description = "Search backed-up files by criteria")
public class SearchController {

    private final SearchManager searchManager;

    @Autowired
    public SearchController(SearchManager searchManager) {
        this.searchManager = searchManager;
    }

    @PostMapping("/backup/search")
    public SearchResponseDTO search(@RequestBody SearchRequestDTO request) throws InvalidSearchRequestException {
        return searchManager.search(request);
    }
}
