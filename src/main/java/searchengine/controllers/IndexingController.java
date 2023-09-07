package searchengine.controllers;


import searchengine.controllers.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.dao.IndexingDao;

@Controller

public class IndexingController {

    private final IndexingDao indexingDao;

    public IndexingController(IndexingDao indexingDao) {
        this.indexingDao = indexingDao;
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        ApiResponse apiResponse = indexingDao.startIndexing();
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        ApiResponse apiResponse = indexingDao.stopIndexing();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/api/indexPage")
    public ResponseEntity<Object> pageIndexing(
            @RequestParam(name="url", required=false, defaultValue=" ") String url) {
        ApiResponse apiResponse = indexingDao.pageIndexing(url);
        return ResponseEntity.ok(apiResponse);
    }
}
