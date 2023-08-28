package searchengine.dao;

import searchengine.controllers.responses.ApiResponse;

public interface IndexingService {
    ApiResponse startIndexing();
    ApiResponse stopIndexing();
    ApiResponse pageIndexing(String url);
}
