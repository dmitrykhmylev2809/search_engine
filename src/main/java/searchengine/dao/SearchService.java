package searchengine.dao;

import searchengine.dto.MorphologyAnalyzerRequestDTO;
import searchengine.controllers.responses.ApiResponse;

import java.io.IOException;

public interface SearchService {
    ApiResponse getResponse (MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, String url, int offset, int limit) throws IOException;
}
