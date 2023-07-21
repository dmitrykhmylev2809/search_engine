package searchengine;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;


public class SiteMapBuilder {

    private final String url;
    private final boolean isInterrupted;

    private final SearchSettings searchSettings;
    private List<String> siteMap;

    public SiteMapBuilder(String url, boolean isInterrupted, SearchSettings searchSettings){
        this.url = url;
        this.isInterrupted = isInterrupted;
        this.searchSettings = searchSettings;
    }

    public void builtSiteMap() {

//        String text = new ForkJoinPool().invoke(new ParseUrl(url, isInterrupted, searchSettings));

       Set<String> urlList = new ForkJoinPool().invoke(new ParseUrl(url, new HashSet<>()));

//       StringBuilder result = new StringBuilder();
//
//        for (String lg : urlList) {
//                String text = lg;
//                if (!text.equals("")) {
//                    result.append("\n");
//                    result.append(text);
//                }
        siteMap = new ArrayList<>(urlList);
//        siteMap = stringToList(text);
    }

    private List<String> stringToList (String text) {
        return Arrays.stream(text.split("\n")).collect(Collectors.toList());
    }

    public List<String> getSiteMap() {
        return siteMap;
    }
}
