package searchengine.url;

import searchengine.config.SearchSettings;
import searchengine.models.*;
import searchengine.morphology.MorphologyAnalyzer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.repo.*;


import java.io.IOException;
import java.util.*;

public class SiteIndexing extends Thread{
    private final Site site;
    private final SearchSettings searchSettings;
    private final FieldRepository fieldRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final boolean allSite;

    public volatile boolean  isStopped;

    public SiteIndexing(Site site,
                        SearchSettings searchSettings,
                        FieldRepository fieldRepository,
                        SiteRepository siteRepository,
                        IndexRepository indexRepository,
                        PageRepository pageRepository,
                        LemmaRepository lemmaRepository,
                        boolean allSite) {
        this.site = site;
        this.searchSettings = searchSettings;
        this.fieldRepository = fieldRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.allSite = allSite;
    }



    @Override
    public void run() {
        try {
            if (allSite) {
                runAllIndexing();
            } else {
                runOneSiteIndexing(site.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void runAllIndexing() {
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepository.save(site);
        SiteMapBuilder builder = new SiteMapBuilder(site.getUrl(), this.isInterrupted(), searchSettings);
        builder.builtSiteMap();
        List<String> allSiteUrls = builder.getSiteMap();
        for(String url : allSiteUrls) {
            runOneSiteIndexing(url);
        }
    }

    public void runOneSiteIndexing(String searchUrl) {
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepository.save(site);
        List<Field> fieldList = getFieldListFromDB();
        try {
             Page page = getSearchPage(searchUrl, site.getUrl(), site.getId());
            Page checkPage = pageRepository.findByPath(searchUrl.replaceAll(site.getUrl(), ""));
            if (checkPage != null){
                prepareDbToIndexing(checkPage);
            }
            TreeMap<String, Integer> map = new TreeMap<>();
            TreeMap<String, Float> indexing = new TreeMap<>();
            for (Field field : fieldList){
                String name = field.getName();
                float weight = field.getWeight();
                String stringByTeg = getStringByTeg(name, page.getContent());
                MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
                TreeMap<String, Integer> tempMap = analyzer.textAnalyzer(stringByTeg);
                map.putAll(tempMap);
                indexing.putAll(indexingLemmas(tempMap, weight));
            }
            lemmaToDB(map, site.getId());
            map.clear();
            pageToDb(page);
            indexingToDb(indexing, page.getPath());
            indexing.clear();

        }
        catch (UnsupportedMimeTypeException e) {
            site.setLastError("Формат страницы не поддерживается: " + searchUrl);
            site.setStatus(Status.FAILED);
        }
        catch (IOException e) {
            site.setLastError("Ошибка чтения страницы: " + searchUrl + "\n" + e.getMessage());
            site.setStatus(Status.FAILED);
        }
        finally {
            siteRepository.save(site);
        }
        if (!isStopped) {
        site.setStatus(Status.INDEXED); }
        else {
            site.setStatus(Status.FAILED);
        }
        siteRepository.save(site);
    }

    private void pageToDb(Page page) {
        pageRepository.save(page);
    }

    private Page getSearchPage(String url, String baseUrl, int siteId) throws IOException {
        Page page = new Page();
        String content = Jsoup.connect(url)
                .userAgent(searchSettings.getAgent())
                .referrer(searchSettings.getReferrer())
                .get()
                .html();

        String path = url.replaceAll(baseUrl, "");
        int code = Jsoup.connect(url).method(Connection.Method.HEAD).execute().statusCode();
        page.setCode(code);
        page.setPath(path);
        page.setContent(content);
        page.setSiteId(siteId);
        return page;
    }

    private List<Field> getFieldListFromDB() {
        List<Field> list = new ArrayList<>();
        Iterable<Field> iterable = fieldRepository.findAll();
        iterable.forEach(list::add);
        return list;
    }

    private String getStringByTeg (String teg, String html) {
        String string = "";
        Document document = Jsoup.parse(html);
        Elements elements = document.select(teg);
        StringBuilder builder = new StringBuilder();
        elements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()){
            string = builder.toString();
        }
        return string;
    }

    private void lemmaToDB (TreeMap<String, Integer> lemmaMap, int siteId) {
        for (Map.Entry<String, Integer> lemma : lemmaMap.entrySet()) {
            String lemmaName = lemma.getKey();
            List<Lemma> lemma1 = lemmaRepository.findByLemma(lemmaName);
            Lemma lemma2 = lemma1.stream().
                    filter(lemma3 -> lemma3.getSiteId() == siteId).
                    findFirst().
                    orElse(null);
            if (lemma2 == null){
                Lemma newLemma = new Lemma(lemmaName, 1, siteId);
                lemmaRepository.save(newLemma);
            } else {
                int count = lemma2.getFrequency();
                lemma2.setFrequency(++count);
                lemmaRepository.save(lemma2);
            }}
    }

    private TreeMap<String, Float> indexingLemmas (TreeMap<String, Integer> lemmas, float weight) {
        TreeMap<String, Float> map = new TreeMap<>();
        for (Map.Entry<String, Integer> lemma : lemmas.entrySet()) {
            String name = lemma.getKey();
            float w;
            if (!map.containsKey(name)) {
                w = (float) lemma.getValue() * weight;
            } else {
                w = map.get(name) + ((float) lemma.getValue() * weight);
            }
            map.put(name, w);
        }
        return map;
    }

    private void indexingToDb (TreeMap<String, Float> map, String path){
        Page page = pageRepository.findByPath(path);
        int pathId = page.getId();
        int siteId = page.getSiteId();
        for (Map.Entry<String, Float> lemma : map.entrySet()) {
            String lemmaName = lemma.getKey();
            List<Lemma> lemma1 = lemmaRepository.findByLemma(lemmaName);
            for (Lemma l : lemma1) {
                if (l.getSiteId() == siteId) {
                    int lemmaId = l.getId();
                    Indexing indexing = new Indexing(pathId, lemmaId, lemma.getValue());
                    indexRepository.save(indexing);
                }
            }}
    }

    private void prepareDbToIndexing(Page page) {
        List<Indexing> indexingList = indexRepository.getAllIndexingByPageId(page.getId());

        List<Integer> lemmaIds = new ArrayList<>();
        for (Indexing indexing : indexingList) {
            lemmaIds.add(indexing.getLemmaId());
        }

        List<Lemma> allLemmasIdByPage = (List<Lemma>) lemmaRepository.findAllById(lemmaIds);
        lemmaRepository.deleteAll(allLemmasIdByPage);
        indexRepository.deleteAll(indexingList);
        pageRepository.delete(page);
    }
}
