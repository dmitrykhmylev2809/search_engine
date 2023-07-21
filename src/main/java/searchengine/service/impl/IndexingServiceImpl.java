package searchengine.service.impl;

import searchengine.Indexer;
import searchengine.service.IndexingService;
import searchengine.service.responses.FalseApiResponse;
import searchengine.service.responses.ApiResponse;
import searchengine.service.responses.TrueApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final Indexer indexer;

    private static final Log log = LogFactory.getLog(IndexingServiceImpl.class);

    public IndexingServiceImpl(Indexer indexer) {
        this.indexer = indexer;
    }

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response;
        boolean indexing;
        try {
            indexing = indexer.allSiteIndexing();
            log.info("Попытка запуска индексации всех сайтов");
        } catch (InterruptedException e) {
            response = new FalseApiResponse("Ошибка запуска индексации");
            log.error("Ошибка запуска индексации", e);
            return response;
        }
        if (indexing) {
            response = new TrueApiResponse();
            log.info("Индексация всех сайтов запущена");
        } else {
            response = new FalseApiResponse("Индексация уже запущена");
            log.warn("Индексация всех сайтов не запущена. Т.к. процесс индексации был запущен ранее.");
        }
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        boolean indexing = indexer.stopSiteIndexing();
        log.info("Попытка остановки индексации");
        ApiResponse response;
        if (indexing) {
            response = new TrueApiResponse();
            log.info("Индексация остановлена");
        } else {
            response = new FalseApiResponse("Индексация не запущена");
            log.warn("Остановка индексации не может быть выполнена, потому что процесс индексации не запущен.");
        }
        return response;
    }

    @Override
    public ApiResponse pageIndexing(String url) {
        ApiResponse resp;
        String response;
        try {
            response = indexer.checkedSiteIndexing(url);
        } catch (InterruptedException e) {
            resp = new FalseApiResponse("Ошибка запуска индексации");
            return resp;
        }

        if (response.equals("not found")) {
            resp = new FalseApiResponse("Страница находится за пределами сайтов," +
                    " указанных в конфигурационном файле");
        }
        else if (response.equals("false")) {
            resp = new FalseApiResponse("Индексация страницы уже запущена");
        }
        else {
            resp = new TrueApiResponse();
        }
        return resp;
    }
}
