package searchengine.controllers;


import searchengine.service.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.service.IndexingService;

@Controller

public class IndexingController {

    private final IndexingService indexingService;

    public IndexingController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        ApiResponse apiResponse = indexingService.startIndexing();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        ApiResponse apiResponse = indexingService.stopIndexing();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/api/indexPage")
    public ResponseEntity<Object> pageIndexing(
            @RequestParam(name="url", required=false, defaultValue=" ") String url) {
        ApiResponse apiResponse = indexingService.pageIndexing(url);
        return ResponseEntity.ok(apiResponse);
    }
}
