package searchengine.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.responses.SearchApiResponse;
import searchengine.morphology.QueryToLemmaList;
import searchengine.responses.FalseApiResponse;
import searchengine.responses.ApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import searchengine.dto.SearchDataDTO;
import searchengine.models.Indexing;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.morphology.MorphologyAnalyzer;
import searchengine.repo.IndexRepository;
import searchengine.repo.LemmaRepository;
import searchengine.repo.PageRepository;
import searchengine.repo.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


@Service
public class SearchServiceImpl implements SearchService {

    private static final Log log = LogFactory.getLog(SearchServiceImpl.class);

    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public SearchServiceImpl(SiteRepository siteRepository,
                             IndexRepository indexRepository,
                             PageRepository pageRepository,
                             LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public ApiResponse getResponse(QueryToLemmaList queryToLemmaList, String url, int offset, int limit) throws IOException {
        log.info("Запрос на поиск строки- \"" + queryToLemmaList.getReq() + "\"");
        if (queryToLemmaList.getReq().equals("")) {
            ApiResponse response = new FalseApiResponse("Задан пустой поисковый запрос");
            log.warn("Задан пустой поисковый запрос");
            return response;
        }
        if (url.equals("")) {
            return searchService(queryToLemmaList, null, offset, limit);
        } else {
            return searchService(queryToLemmaList, url, offset, limit);
        }
    }

    private ApiResponse searchService(QueryToLemmaList query, String url, int offset, int limit) {
        List<Site> siteList = siteRepository.findAll();
        List<SearchDataDTO> listOfSearchData = new ArrayList<>();
        if (url == null) {
            for (Site s : siteList) {
                Map<Page, Double> list = searching(query, s.getId());

                listOfSearchData.addAll(getSortedSearchData(list, query));
            }
        } else {
            Site site = siteRepository.findByUrl(url);
            Map<Page, Double> list = searching(query, site.getId());
            listOfSearchData.addAll(getSortedSearchData(list, query));
        }
        int count;
        listOfSearchData.sort(Comparator.comparingDouble(SearchDataDTO::getRelevance));
        if (listOfSearchData.isEmpty()) {
            return new SearchApiResponse(false);
        }
        if (limit + offset < listOfSearchData.size()) {
            count = limit;
        } else {
            count = listOfSearchData.size() - offset;
        }
        SearchDataDTO[] searchData = new SearchDataDTO[count];
        for (int i = offset; i < count; i++) {
            searchData[i] = listOfSearchData.get(i);
        }
        return new SearchApiResponse(true, count, searchData);
    }

    private Map<Page, Double> searching(QueryToLemmaList queryToLemmaList, int siteId) {
        HashMap<Page, Double> pageRelevance = new HashMap<>();
        List<Lemma> reqLemmas = sortedReqLemmas(queryToLemmaList, siteId);
        List<Integer> pageIndexes = new ArrayList<>();

        if (!reqLemmas.isEmpty()) {
            pageIndexes = getPageIndexes(reqLemmas);
            Map<Page, Double> pageAbsRelevance = calculatePageAbsRelevance(pageIndexes, reqLemmas, siteId);

            double maxRel = 0.0;
            for (Double rel : pageAbsRelevance.values()) {
                maxRel = Math.max(maxRel, rel);
            }

            normalizePageRelevance(pageAbsRelevance, pageRelevance, maxRel);
        }

        return pageRelevance;
    }

    private List<Integer> getPageIndexes(List<Lemma> reqLemmas) {
        List<Integer> pageIndexes = new ArrayList<>();
        List<Indexing> indexingList = indexRepository.getAllIndexingByLemmaId(reqLemmas.get(0).getId());

        indexingList.forEach(indexing -> pageIndexes.add(indexing.getPageId()));

        for (Lemma lemma : reqLemmas) {
            if (!pageIndexes.isEmpty() && lemma.getId() != reqLemmas.get(0).getId()) {
                List<Indexing> indexingList2 = indexRepository.getAllIndexingByLemmaId(lemma.getId());
                List<Integer> tempList = new ArrayList<>();
                indexingList2.forEach(indexing -> tempList.add(indexing.getPageId()));
                pageIndexes.retainAll(tempList);
            }
        }
        return pageIndexes;
    }

    private Map<Page, Double> calculatePageAbsRelevance(List<Integer> pageIndexes, List<Lemma> reqLemmas, int siteId) {
        Map<Page, Double> pageAbsRelevance = new HashMap<>();
        double maxRel = 0.0;

        for (Integer p : pageIndexes) {
            Optional<Page> opPage = pageRepository.findByIdAndSiteId(p, siteId);

            if (opPage.isPresent()) {
                Page page = opPage.get();
                double r = getAbsRelevance(page, reqLemmas);
                pageAbsRelevance.put(page, r);
                maxRel = Math.max(maxRel, r);
            }
        }

        return pageAbsRelevance;
    }

    private void normalizePageRelevance(Map<Page, Double> pageAbsRelevance, Map<Page, Double> pageRelevance, double maxRel) {
        for (Map.Entry<Page, Double> abs : pageAbsRelevance.entrySet()) {
            pageRelevance.put(abs.getKey(), abs.getValue() / maxRel);
        }
    }



//    private Map<Page, Double> searching(QueryToLemmaList queryToLemmaList, int siteId) {
//        HashMap<Page, Double> pageRelevance = new HashMap<>();
//        List<Lemma> reqLemmas = sortedReqLemmas(queryToLemmaList, siteId);
//        List<Integer> pageIndexes = new ArrayList<>();
//        if (!reqLemmas.isEmpty()) {
//            List<Indexing> indexingList = indexRepository.getAllIndexingByLemmaId(reqLemmas.get(0).getId());
//            indexingList.forEach(indexing -> pageIndexes.add(indexing.getPageId()));
//            for (Lemma lemma : reqLemmas) {
//                if (!pageIndexes.isEmpty() && lemma.getId() != reqLemmas.get(0).getId()) {
//                    List<Indexing> indexingList2 = indexRepository.getAllIndexingByLemmaId(lemma.getId());
//                    List<Integer> tempList = new ArrayList<>();
//                    indexingList2.forEach(indexing -> tempList.add(indexing.getPageId()));
//                    pageIndexes.retainAll(tempList);
//                }
//            }
//            Map<Page, Double> pageAbsRelevance = new HashMap<>();
//
//            double maxRel = 0.0;
//            for (Integer p : pageIndexes) {
//                Optional<Page> opPage;
//                opPage = pageRepository.findByIdAndSiteId(p, siteId);
//                if (opPage.isPresent()) {
//                    Page page = opPage.get();
//                    double r = getAbsRelevance(page, reqLemmas);
//                    pageAbsRelevance.put(page, r);
//                    if (r > maxRel)
//                        maxRel = r;
//                }
//            }
//            for (Map.Entry<Page, Double> abs : pageAbsRelevance.entrySet()) {
//                pageRelevance.put(abs.getKey(), abs.getValue() / maxRel);
//            }
//        }
//        return pageRelevance;
//    }

    private List<SearchDataDTO> getSortedSearchData(Map<Page, Double> sortedPageMap, QueryToLemmaList queryToLemmaList) {
        List<SearchDataDTO> responses = new ArrayList<>();
        LinkedHashMap<Page, Double> sortedByRankPages = (LinkedHashMap<Page, Double>) sortMapByValue(sortedPageMap);
        for (Map.Entry<Page, Double> page : sortedByRankPages.entrySet()) {
            SearchDataDTO response = getResponseByPage(page.getKey(), queryToLemmaList, page.getValue());
            responses.add(response);
        }
        return responses;
    }

    private List<Lemma> sortedReqLemmas(QueryToLemmaList queryToLemmaList, int siteId) {
        List<Lemma> lemmaList = new ArrayList<>();
        List<String> list = queryToLemmaList.getReqLemmas();
        for (String s : list) {
            List<Lemma> reqLemmas = lemmaRepository.findByLemma(s);
            for (Lemma l : reqLemmas) {
                if (l.getSiteId() == siteId) {
                    lemmaList.add(l);
                }
            }
        }
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private double getAbsRelevance(Page page, List<Lemma> lemmas) {
        double r = 0.0;
        int pageId = page.getId();
        for (Lemma lemma : lemmas) {
            int lemmaId = lemma.getId();
            Indexing indexing = indexRepository.findByLemmaIdAndPageId(lemmaId, pageId);
            r = r + indexing.getRank();
        }
        return r;
    }


    private SearchDataDTO getResponseByPage(Page page, QueryToLemmaList queryToLemmaList, double relevance) {
        SearchDataDTO response = new SearchDataDTO();
        Site site = siteRepository.findById(page.getSiteId());
        String siteUrl = site.getUrl();
        String siteName = site.getName();
        String uri = page.getPath();
        String title = getTitle(page.getContent());
        String snippet = getSnippet(page.getContent(), queryToLemmaList);
        response.setSite(siteUrl);
        response.setSiteName(siteName);
        response.setRelevance(relevance);
        response.setUri(uri);
        response.setTitle(title);
        response.setSnippet(snippet);
        return response;
    }

    private String getTitle(String html) {
        String string = "";
        Document document = Jsoup.parse(html);
        Elements elements = document.select("title");
        StringBuilder builder = new StringBuilder();
        elements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()) {
            string = builder.toString();
        }
        return string;
    }

    private String getSnippet(String html, QueryToLemmaList queryToLemmaList) {
        MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
        String string = extractTextFromHtml(html);
        List<String> req = queryToLemmaList.getReqLemmas();
        Map<String, Integer> lemmaIndexes = calculateLemmaIndexes(string, req, analyzer);
        LinkedHashMap<String, Integer> sortedLemmaIndexes = sortLinkedMapByValue(lemmaIndexes);
        return constructSnippet(string, sortedLemmaIndexes);
    }

    private String extractTextFromHtml(String html) {
        Document document = Jsoup.parse(html);
        Elements titleElements = document.select("title");
        Elements bodyElements = document.select("body");
        StringBuilder builder = new StringBuilder();

        titleElements.forEach(element -> builder.append(element.text()).append(" ").append("\n"));
        bodyElements.forEach(element -> builder.append(element.text()).append(" "));

         if (!builder.isEmpty()) {
            return builder.toString();
        } else {
            return "";
        }
    }

    private Map<String, Integer> calculateLemmaIndexes(String text, List<String> req, MorphologyAnalyzer analyzer) {
        Map<String, Integer> lemmaIndexes = new HashMap<>();

        for (String s : req) {
            lemmaIndexes.put(s, analyzer.findLemmaIndexInText(text, s).get(0));
        }

        return lemmaIndexes;
    }

    private String constructSnippet(String text, LinkedHashMap<String, Integer> sortedLemmaIndexes) {
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, Integer>> iterator = sortedLemmaIndexes.entrySet().iterator();
        Map.Entry<String, Integer> prevEntry = iterator.next();
        int shorts = sortedLemmaIndexes.size();

        while (iterator.hasNext()) {
            Map.Entry<String, Integer> currentEntry = iterator.next();
            int currentValue = currentEntry.getValue();
            int prevValue = prevEntry.getValue();
            int difference = currentValue - prevValue;
            int endIndex = findEndIndex(text, prevValue);
            int to = endIndex;
            String snippet = createSnippet(text, prevValue, to, currentValue, difference, shorts);
            builder.append(snippet);
            prevEntry = currentEntry;
        }

        int endIndex = findEndIndex(text, prevEntry.getValue());
        int to = endIndex;
        int endShort = calculateEndShort(builder, text, prevEntry, to, shorts);
        String snippet = createEndSnippet(text, prevEntry.getValue(), to, endShort);
        builder.append(snippet);
        return builder.toString();
    }

    private int findEndIndex(String text, int prevValue) {
        int endIndex = -1;
        if (!text.isEmpty()) {
            endIndex = text.indexOf(" ", prevValue + 1);
        }
        if (endIndex == -1) {
            endIndex = text.length();
        }
        return endIndex;
    }

    private String createSnippet(String text, int prevValue, int to, int currentValue, int difference, int shorts) {
        if (difference < 240 / shorts) {
            return "<b>" + text.substring(prevValue, to) + "</b>" + text.substring(to, currentValue - 1) + " ";
        } else {
            return "<b>" + text.substring(prevValue, to) + "</b>" + text.substring(to, to + 240 / shorts) + "... ";
        }
    }

    private int calculateEndShort(StringBuilder builder, String text, Map.Entry<String, Integer> prevEntry, int to, int shorts) {
        int endShort = 240 - builder.length() - (to - prevEntry.getValue()) - 3;
        return endShort;
    }

    private String createEndSnippet(String text, int prevValue, int to, int endShort) {
        return "<b>" + text.substring(prevValue, to) + "</b>" + text.substring(to, to + endShort) + "... ";
    }

    public <K, V extends Comparable<? super V>> Map<K, V>
    sortMapByValue(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K, V>> st = map.entrySet().stream();

        st.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> result.put(e.getKey(), e.getValue()));

        return result;
    }

    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortLinkedMapByValue(Map<K, V> map) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K, V>> st = map.entrySet().stream();

        st.sorted(Map.Entry.comparingByValue())
                .forEach(e -> result.put(e.getKey(), e.getValue()));

        return result;
    }

}
