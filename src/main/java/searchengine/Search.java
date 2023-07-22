package searchengine;

import searchengine.DTO.MorphologyAnalyzerRequestDTO;
import searchengine.models.*;
import searchengine.morphology.MorphologyAnalyzer;
import searchengine.service.IndexRepositoryService;
import searchengine.service.LemmaRepositoryService;
import searchengine.service.PageRepositoryService;
import searchengine.service.SiteRepositoryService;
import searchengine.service.responses.SearchApiResponse;
import searchengine.DTO.SearchDataDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public SearchApiResponse searchService (MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, String url, int offset, int limit) {
        List<Site> siteList = siteRepositoryService.getAllSites();
        List<SearchDataDTO> listOfSearchData = new ArrayList<>();
        if(url == null) {
            for(Site s : siteList){
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
        if (listOfSearchData.isEmpty()){
            return new SearchApiResponse(false);
        }
        if(limit + offset < listOfSearchData.size()) {
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

    private List<SearchDataDTO> getSortedSearchData (Map<Page, Double> sortedPageMap, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO) {
        List<SearchDataDTO> responses = new ArrayList<>();
        LinkedHashMap<Page, Double> sortedByRankPages = (LinkedHashMap<Page, Double>) sortMapByValue(sortedPageMap);
        for (Map.Entry<Page, Double> page : sortedByRankPages.entrySet()) {
            SearchDataDTO response = getResponseByPage(page.getKey(), morphologyAnalyzerRequestDTO, page.getValue());
            responses.add(response);
        }
        return responses;
    }

    private List<Lemma> sortedReqLemmas(MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, int siteId){
        List<Lemma> lemmaList = new ArrayList<>();
        List<String> list = morphologyAnalyzerRequestDTO.getReqLemmas();
        for(String s : list) {
            List<Lemma> reqLemmas = lemmaRepositoryService.getLemma(s);
            for(Lemma l : reqLemmas){
                if(l.getSiteId() == siteId){
                    lemmaList.add(l);
                }
            }
        }
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private double getAbsRelevance(Page page, List<Lemma> lemmas){
        double r = 0.0;
        int pageId = page.getId();
        for (Lemma lemma : lemmas) {
            int lemmaId = lemma.getId();
            Indexing indexing = indexRepositoryService.getIndexing(lemmaId, pageId);
            r = r + indexing.getRank();
        }
        return r;
    }


    private SearchDataDTO getResponseByPage (Page page, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO, double relevance) {
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

    private String getTitle (String html){
        String string = "";
        Document document = Jsoup.parse(html);
        Elements elements = document.select("title");
        StringBuilder builder = new StringBuilder();
        elements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()){
            string = builder.toString();
        }
        return string;
    }

    private String getSnippet (String html, MorphologyAnalyzerRequestDTO morphologyAnalyzerRequestDTO) {
        MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
        String string = "";
        Document document = Jsoup.parse(html);
        Elements titleElements = document.select("title");
        Elements bodyElements = document.select("body");
        StringBuilder builder = new StringBuilder();
        titleElements.forEach(element -> builder.append(element.text()).append(" ").append("\n"));
        bodyElements.forEach(element -> builder.append(element.text()).append(" "));
        if (!builder.isEmpty()){
            string = builder.toString();
        }
        List<String> req = morphologyAnalyzerRequestDTO.getReqLemmas();
        Map<String, Integer> integerList1 = new HashMap<>();
 //       Set<Integer> integerList = new TreeSet<>();
        for (String s : req) {
            integerList1.put(s, analyzer.findLemmaIndexInText(string, s).get(0));
 //           integerList.addAll(analyzer.findLemmaIndexInText(string, s));
        }

//        for (int key : integerList) {
//
//            int lengthShorts = 0;
//            while (lengthShorts > 0 && !Character.isWhitespace(string.charAt(lengthShorts))) {
//                lengthShorts++;
//            }
//
//            String part1 = string.substring(0, key);
//            String part2 = string.substring(key, lengthShorts);
//            String part3 = string.substring(lengthShorts);
//
//            string = part1 + "<b>" + part2 + "</b>" + part3;
//
//        }



        //       List<TreeSet<Integer>> indexesList = getSearchingIndexes(string, integerList);
        StringBuilder builder1 = new StringBuilder();
//        for (TreeSet<Integer> set : indexesList) {
//            int from = set.first();
//            int to = set.last();
//            Pattern pattern = Pattern.compile("\\p{Punct}|\\s");
//            Matcher matcher = pattern.matcher(string.substring(to));
//            int offset = 0;
//            if (matcher.find()){
//                offset = matcher.end();
//            }
//            builder1.append("<b>")
//                    .append(string, from, to + offset)
//                    .append("</b>");
//            if (!((string.length() - to) < 40)) {
//                builder1.append(string, to + offset, string.indexOf(" ", to + offset + 100))
//                        .append("... ");
//            }
//        }


 //       int totalLength = 0;
 //       boolean ellipsisAdded = false;



//        String snippet = "";
//        int reqQuantity = req.size();
//        for (TreeSet<Integer> set : indexesList) {
//            int from = set.first();
//            int to = set.last();
//            Pattern pattern = Pattern.compile("\\p{Punct}|\\s");
//            Matcher matcher = pattern.matcher(string.substring(to));
//            int offset = 0;
//            if (matcher.find()) {
//                offset = matcher.end();
//            }
//
//            if (indexesList.size() != 1 && reqQuantity != 1) {
//                snippet = "<b>" + string.substring(from, to + offset) + "</b>" + string.substring(to + offset);
//                if (snippet.length() > 130) {
//                    snippet = snippet.substring (0, 127) + " ... ";
//                } else { snippet = snippet + "..."; }
//            }
//            else {
//
//                snippet = "<b>" + string.substring(from, to + offset) + "</b>" + string.substring(to + offset + 1, to + 250) + " ... ";
//            }
//                builder1.append(snippet);
//        }


            for (String val : integerList1.keySet()) {
                int index = integerList1.get(val);
                int shorts = integerList1.size();
                int to = index + val.length();
            int lengthShorts = to + 240 / shorts;
            while (lengthShorts > 0 && !Character.isWhitespace(string.charAt(lengthShorts))) {
                lengthShorts--;
            }

//            String snippet = string.substring(key, lengthShorts);
            String snippet = "<b>" + string.substring(index, to) + "</b>" + string.substring(to, lengthShorts);
//            if (to < string.length()) {
//                snippet += string.substring(to, to + 1) + " ... ";
//            } else {
                snippet += "...";
 //           }

            builder1.append(snippet);
        }
        string = builder1.toString();
        for (String val : integerList1.keySet()) {
            string = string.replaceAll(val,"<b>" + val + "</b>" );

        }
        string = string.replaceAll("<b>" + "<b>","<b>");
        string = string.replaceAll("</b>" + "</b>","</b>" );

        return string;
    }


    private List<TreeSet<Integer>> getSearchingIndexes (String string, Set<Integer> indexesOfBolt) {
        ArrayList<Integer> indexes = new ArrayList<>(indexesOfBolt);
        List<TreeSet<Integer>> list = new ArrayList<>();
        TreeSet<Integer> temp = new TreeSet<>();
        for (int i = 0; i < indexes.size(); i++) {
            String s = string.substring(indexes.get(i));
            int end = s.indexOf(" ");
            if ((i + 1) <= indexes.size() - 1 && (indexes.get(i + 1) - indexes.get(i)) < end + 5){
                temp.add(indexes.get(i));
                temp.add(indexes.get(i + 1));
            }
             else {
                if (!temp.isEmpty()) {
                    list.add(temp);
                    temp = new TreeSet<>();
                }
                temp.add(indexes.get(i));
                list.add(temp);
                temp = new TreeSet<>();
            }
        }
        list.sort((Comparator<Set<Integer>>) (o1, o2) -> o2.size() - o1.size());
        ArrayList<TreeSet<Integer>> searchingIndexes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if(list.size() > i) {
                searchingIndexes.add(list.get(i));
            }
        }
        return searchingIndexes;
    }

    public  <K, V extends Comparable<? super V>> Map<K, V>
    sortMapByValue(Map<K, V> map )
    {
        Map<K,V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K,V>> st = map.entrySet().stream();

        st.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(e ->result.put(e.getKey(),e.getValue()));

        return result;
    }
}
