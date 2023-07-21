package searchengine.service.impl;

import searchengine.Search;
import searchengine.DTO.MorphologyAnalyzerRequestDTO;
import searchengine.service.SearchService;
import searchengine.service.responses.FalseApiResponse;
import searchengine.service.responses.ApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Log log = LogFactory.getLog(SearchServiceImpl.class);

    private final Search search;

    public SearchServiceImpl(Search search) {
        this.search = search;
    }

    ApiResponse response;

    @Override
    public ApiResponse getResponse(MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, String url, int offset, int limit) throws IOException {
        log.info("Запрос на поиск строки- \"" + morphologyAnalyzerRequestDTO.getReq() + "\"");
        if (morphologyAnalyzerRequestDTO.getReq().equals("")){
            response = new FalseApiResponse("Задан пустой поисковый запрос");
            log.warn("Задан пустой поисковый запрос");
            return response;
            }
        if(url.equals("")) {
            response = search.searchService(morphologyAnalyzerRequestDTO, null, offset, limit);
        } else {
            response = search.searchService(morphologyAnalyzerRequestDTO, url, offset, limit);
        }
        if (response.getResult()) {
            log.info("Запрос на поиск строки обработан, результат получен.");
            return response;
        } else {
            log.warn("Запрос на поиск строки обработан, указанная страница не найдена.");
            return new FalseApiResponse("Указанная страница не найдена");
        }
    }
}
