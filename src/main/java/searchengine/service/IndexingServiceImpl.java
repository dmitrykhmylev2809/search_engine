package searchengine.service;

import org.springframework.stereotype.Service;
import searchengine.config.SearchSettings;
import searchengine.responses.FalseApiResponse;
import searchengine.responses.ApiResponse;
import searchengine.responses.TrueApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import searchengine.models.Field;
import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.repo.*;
import searchengine.url.SiteIndexing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SearchSettings searchSettings;
    private final FieldRepository fieldRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public volatile boolean  isStopped;

    private static final Log log = LogFactory.getLog(IndexingServiceImpl.class);

    private final ThreadPoolExecutor executor;

    private final List<Future<?>> futures = new ArrayList<>();

    public IndexingServiceImpl(SearchSettings searchSettings, FieldRepository fieldRepository,
                               SiteRepository siteRepository, IndexRepository indexRepository, PageRepository pageRepository,
                               LemmaRepository lemmaRepository) {
        this.searchSettings = searchSettings;
        this.fieldRepository = fieldRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    }

    @Override
    public ApiResponse startIndexing() {
        ApiResponse response;
        boolean indexing;
        try {
            indexing = allSiteIndexing();
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
        boolean indexing = stopSiteIndexing();
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
            response = checkedSiteIndexing(url);
        } catch (InterruptedException e) {
            resp = new FalseApiResponse("Ошибка запуска индексации");
            return resp;
        }

        if (response.equals("not found")) {
            resp = new FalseApiResponse("Страница находится за пределами сайтов," +
                    " указанных в конфигурационном файле");
        } else if (response.equals("false")) {
            resp = new FalseApiResponse("Индексация страницы уже запущена");
        } else {
            resp = new TrueApiResponse();
        }
        return resp;
    }

    private boolean allSiteIndexing() throws InterruptedException {
        fieldInit();
        boolean isIndexing;
        List<Site> siteList = getSiteListFromConfig();
        for (Site site : siteList) {
            isIndexing = startSiteIndexing(site);
            if (!isIndexing){
                stopSiteIndexing();
                return false;
            }
        }
        return true;
    }

    private void fieldInit() {
        Field fieldTitle = new Field("title", "title", 1.0f);
        Field fieldBody = new Field("body", "body", 0.8f);
        if (fieldRepository.getFieldByName("title") == null) {
            fieldRepository.save(fieldTitle);
            fieldRepository.save(fieldBody);
        }
    }

     private String checkedSiteIndexing(String url) throws InterruptedException {
        List<Site> siteList = siteRepository.findAll();
        String baseUrl = "";
        for(Site site : siteList) {
            if(site.getStatus() != Status.INDEXED) {
                return "false";
            }
            if(url.contains(site.getUrl())){
                baseUrl = site.getUrl();
            }
        }
        if(baseUrl.isEmpty()){
            return "not found";
        } else {
            Site site = siteRepository.findByUrl(baseUrl);
            site.setUrl(url);
            SiteIndexing indexing = new SiteIndexing(
                    site,
                    searchSettings,
                    fieldRepository,
                    siteRepository,
                    indexRepository,
                    pageRepository,
                    lemmaRepository,
                    false);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            site.setUrl(baseUrl);
            siteRepository.save(site);
            return "true";
        }
    }

    private boolean startSiteIndexing(Site site) throws InterruptedException {
        Site site1 = siteRepository.findByUrl(site.getUrl());
        if (site1 == null) {
            siteRepository.save(site);
            SiteIndexing indexing = new SiteIndexing(
                    siteRepository.findByUrl(site.getUrl()),
                    searchSettings,
                    fieldRepository,
                    siteRepository,
                    indexRepository,
                    pageRepository,
                    lemmaRepository,
                    true);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            return true;
        } else {
            if (!site1.getStatus().equals(Status.INDEXING)){
                SiteIndexing indexing = new SiteIndexing(
                        siteRepository.findByUrl(site.getUrl()),
                        searchSettings,
                        fieldRepository,
                        siteRepository,
                        indexRepository,
                        pageRepository,
                        lemmaRepository,
                        true);
                Future<?> future = executor.submit(indexing);
                futures.add(future);

                return true;
            } else {
                return false;
            }
        }
    }

    public boolean stopSiteIndexing(){
        boolean isThreadAlive = false;
        isStopped = true;
        if(executor.getActiveCount() == 0){
            return false;
        }
        try {
            for (Future<?> future : futures) {
                future.cancel(true);
                executor.shutdownNow();
                isThreadAlive = executor.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Ошибка закрытия потоков: " + e);
        }
        if (!isThreadAlive){
            List<Site> siteList = siteRepository.findAll();
            for(Site site : siteList) {
                site.setStatus(Status.FAILED);
                siteRepository.save(site);
            }
        }
        return !isThreadAlive;
    }

    private List<Site> getSiteListFromConfig() {
        List<Site> siteList = new ArrayList<>();
        List<HashMap<String, String>> sites = searchSettings.getSite();
        for (HashMap<String, String> map : sites) {
            String url = "";
            String name = "";
            for (Map.Entry<String, String> siteInfo : map.entrySet()) {
                if (siteInfo.getKey().equals("name")) {
                    name = siteInfo.getValue();
                }
                if (siteInfo.getKey().equals("url")) {
                    url = siteInfo.getValue();
                }
            }
            Site site = new Site();
            site.setUrl(url);
            site.setName(name);
            site.setStatus(Status.FAILED);
            siteList.add(site);
        }
        return siteList;
    }

}
