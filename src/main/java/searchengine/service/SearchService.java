package searchengine.service;

import searchengine.dto.MorphologyAnalyzerRequestDTO;
import searchengine.controllers.responses.ApiResponse;

import java.io.IOException;

public interface SearchService {
    ApiResponse getResponse(MorphologyAnalyzerRequestDTO query, String site, int offset, int limit) throws IOException;
}
