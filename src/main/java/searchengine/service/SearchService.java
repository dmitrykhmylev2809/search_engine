package searchengine.service;

import searchengine.DTO.MorphologyAnalyzerRequestDTO;
import searchengine.service.responses.ApiResponse;

import java.io.IOException;

public interface SearchService {
    ApiResponse getResponse (MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, String url, int offset, int limit) throws IOException;
}
