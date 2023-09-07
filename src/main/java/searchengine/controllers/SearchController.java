package searchengine.controllers;

import searchengine.dto.MorphologyAnalyzerRequestDTO;
import searchengine.dao.SearchDao;
import searchengine.controllers.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class SearchController {

    private final SearchDao searchDao;

    public SearchController(SearchDao searchDao) {
        this.searchDao = searchDao;
    }

    @GetMapping("/api/search")
    public ResponseEntity<Object> search(
            @RequestParam(name="query", required=false, defaultValue="") String query,
            @RequestParam(name="site", required=false, defaultValue="") String site,
            @RequestParam(name="offset", required=false, defaultValue="0") int offset,
            @RequestParam(name="limit", required=false, defaultValue="0") int limit) throws IOException {
        ApiResponse apiResponse = searchDao.getResponse(new MorphologyAnalyzerRequestDTO(query), site, offset, 40);
        return ResponseEntity.ok (apiResponse);
    }
}
