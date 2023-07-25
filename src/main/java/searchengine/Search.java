package searchengine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.DTO.MorphologyAnalyzerRequestDTO;
import searchengine.DTO.SearchDataDTO;
import searchengine.models.Indexing;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.morphology.MorphologyAnalyzer;
import searchengine.service.IndexRepositoryService;
import searchengine.service.LemmaRepositoryService;
import searchengine.service.PageRepositoryService;
import searchengine.service.SiteRepositoryService;
import searchengine.service.responses.SearchApiResponse;

import java.util.*;
import java.util.stream.Stream;

@Service
public class Search {

    private final SiteRepositoryService siteRepositoryService;
    private final IndexRepositoryService indexRepositoryService;
    private final PageRepositoryService pageRepositoryService;
    private final LemmaRepositoryService lemmaRepositoryService;

    public Search(SiteRepositoryService siteRepositoryService,
                  IndexRepositoryService indexRepositoryService,
                  PageRepositoryService pageRepositoryService,
                  LemmaRepositoryService lemmaRepositoryService) {
        this.siteRepositoryService = siteRepositoryService;
        this.indexRepositoryService = indexRepositoryService;
        this.pageRepositoryService = pageRepositoryService;
        this.lemmaRepositoryService = lemmaRepositoryService;
    }

    public SearchApiResponse searchService(MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, String url, int offset, int limit) {
        List<Site> siteList = siteRepositoryService.getAllSites();
        List<SearchDataDTO> listOfSearchData = new ArrayList<>();
        if (url == null) {
            for (Site s : siteList) {
                Map<Page, Double> list = searching(morphologyAnalyzerRequestDTO, s.getId());

                listOfSearchData.addAll(getSortedSearchData(list, morphologyAnalyzerRequestDTO));
            }
        } else {
            Site site = siteRepositoryService.getSite(url);
            Map<Page, Double> list = searching(morphologyAnalyzerRequestDTO, site.getId());
            listOfSearchData.addAll(getSortedSearchData(list, morphologyAnalyzerRequestDTO));
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

    private Map<Page, Double> searching(MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, int siteId) {
        HashMap<Page, Double> pageRelevance = new HashMap<>();
        List<Lemma> reqLemmas = sortedReqLemmas(morphologyAnalyzerRequestDTO, siteId);
        List<Integer> pageIndexes = new ArrayList<>();
        if (!reqLemmas.isEmpty()) {
            List<Indexing> indexingList = indexRepositoryService.getAllIndexingByLemmaId(reqLemmas.get(0).getId());
            indexingList.forEach(indexing -> pageIndexes.add(indexing.getPageId()));
            for (Lemma lemma : reqLemmas) {
                if (!pageIndexes.isEmpty() && lemma.getId() != reqLemmas.get(0).getId()) {
                    List<Indexing> indexingList2 = indexRepositoryService.getAllIndexingByLemmaId(lemma.getId());
                    List<Integer> tempList = new ArrayList<>();
                    indexingList2.forEach(indexing -> tempList.add(indexing.getPageId()));
                    pageIndexes.retainAll(tempList);
                }
            }
            Map<Page, Double> pageAbsRelevance = new HashMap<>();

            double maxRel = 0.0;
            for (Integer p : pageIndexes) {
                Optional<Page> opPage;
                opPage = pageRepositoryService.findPageByPageIdAndSiteId(p, siteId);
                if (opPage.isPresent()) {
                    Page page = opPage.get();
                    double r = getAbsRelevance(page, reqLemmas);
                    pageAbsRelevance.put(page, r);
                    if (r > maxRel)
                        maxRel = r;
                }
            }
            for (Map.Entry<Page, Double> abs : pageAbsRelevance.entrySet()) {
                pageRelevance.put(abs.getKey(), abs.getValue() / maxRel);
            }
        }

        return pageRelevance;
    }

    private List<SearchDataDTO> getSortedSearchData(Map<Page, Double> sortedPageMap, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO) {
        List<SearchDataDTO> responses = new ArrayList<>();
        LinkedHashMap<Page, Double> sortedByRankPages = (LinkedHashMap<Page, Double>) sortMapByValue(sortedPageMap);
        for (Map.Entry<Page, Double> page : sortedByRankPages.entrySet()) {
            SearchDataDTO response = getResponseByPage(page.getKey(), morphologyAnalyzerRequestDTO, page.getValue());
            responses.add(response);
        }
        return responses;
    }

    private List<Lemma> sortedReqLemmas(MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, int siteId) {
        List<Lemma> lemmaList = new ArrayList<>();
        List<String> list = morphologyAnalyzerRequestDTO.getReqLemmas();
        for (String s : list) {
            List<Lemma> reqLemmas = lemmaRepositoryService.getLemma(s);
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
            Indexing indexing = indexRepositoryService.getIndexing(lemmaId, pageId);
            r = r + indexing.getRank();
        }
        return r;
    }


    private SearchDataDTO getResponseByPage(Page page, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, double relevance) {
        SearchDataDTO response = new SearchDataDTO();
        Site site = siteRepositoryService.getSite(page.getSiteId());
        String siteUrl = site.getUrl();
        String siteName = site.getName();
        String uri = page.getPath();
        String title = getTitle(page.getContent());
        String snippet = getSnippet(page.getContent(), morphologyAnalyzerRequestDTO);
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

    private String getSnippet(String html, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO) {
        MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
        String string = "";
        Document document = Jsoup.parse(html);
        Elements titleElements = document.select("title");
        Elements bodyElements = document.select("body");
        StringBuilder builder = new StringBuilder();
        titleElements.forEach(element -> builder.append(element.text()).append(" ").append("\n"));
        bodyElements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()) {
            string = builder.toString();
        }
        List<String> req = morphologyAnalyzerRequestDTO.getReqLemmas();
        Map<String, Integer> integerList1 = new HashMap<>();

        for (String s : req) {
            integerList1.put(s, analyzer.findLemmaIndexInText(string, s).get(0));
        }
        LinkedHashMap<String, Integer> sortedLinkedLemmasIndexes = sortLinkedMapByValue(integerList1);

        StringBuilder builder1 = new StringBuilder();

        Iterator<Map.Entry<String, Integer>> iterator = sortedLinkedLemmasIndexes.entrySet().iterator();
        Map.Entry<String, Integer> prevEntry = iterator.next();
        int shorts = integerList1.size();

        while (iterator.hasNext()) {
            Map.Entry<String, Integer> currentEntry = iterator.next();
            int currentValue = currentEntry.getValue();
            int prevValue = prevEntry.getValue();
            int difference = currentValue - prevValue;

            int endIndex = string.indexOf(" ", prevValue + 1);
            if (endIndex == -1) {
                endIndex = string.length();
            }

            int to = endIndex;
            if (difference < 240 / shorts) {
                String snippet = "<b>" + string.substring(prevValue, to) + "</b>" + string.substring(to, currentValue - 1) + " ";
                builder1.append(snippet);
            } else {
                String snippet = "<b>" + string.substring(prevValue, to) + "</b>" + string.substring(to, to + 240 / shorts);
                snippet += "... ";
                builder1.append(snippet);
            }
            prevEntry = currentEntry;

        }
        int endIndex = string.indexOf(" ", prevEntry.getValue() + 1);
        if (endIndex == -1) {
            endIndex = string.length();
        }
        int to = endIndex;
        int endShort = 240 - builder1.length() - (to - prevEntry.getValue()) - 3;
        String snippet = "<b>" + string.substring(prevEntry.getValue(), to) + "</b>" + string.substring(to, to + endShort);
        snippet += "... ";

        builder1.append(snippet);
        string = builder1.toString();

        return string;
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
