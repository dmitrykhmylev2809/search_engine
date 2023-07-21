package searchengine.controllers;

import searchengine.DTO.MorphologyAnalyzerRequestDTO;
import searchengine.service.SearchService;
import searchengine.service.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public ResponseEntity<Object> search(
            @RequestParam(name="query", required=false, defaultValue="") String query,
            @RequestParam(name="site", required=false, defaultValue="") String site,
            @RequestParam(name="offset", required=false, defaultValue="0") int offset,
            @RequestParam(name="limit", required=false, defaultValue="0") int limit) throws IOException {
        ApiResponse apiResponse = searchService.getResponse(new MorphologyAnalyzerRequestDTO(query), site, offset, 40);
        return ResponseEntity.ok (apiResponse);
    }
}
