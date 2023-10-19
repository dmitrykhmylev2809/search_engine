package searchengine.service;

import searchengine.responses.ApiResponse;

public interface IndexingService {
    ApiResponse startIndexing();
    ApiResponse stopIndexing();
    ApiResponse pageIndexing(String url);
}
