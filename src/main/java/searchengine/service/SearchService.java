package searchengine.service;

import searchengine.morphology.QueryToLemmaList;
import searchengine.controllers.responses.ApiResponse;

import java.io.IOException;

public interface SearchService {
    ApiResponse getResponse(QueryToLemmaList query, String site, int offset, int limit) throws IOException;
}
