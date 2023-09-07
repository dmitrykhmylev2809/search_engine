package searchengine.dao;

import searchengine.controllers.responses.ApiResponse;

public interface IndexingDao {
    ApiResponse startIndexing();
    ApiResponse stopIndexing();
    ApiResponse pageIndexing(String url);
}
