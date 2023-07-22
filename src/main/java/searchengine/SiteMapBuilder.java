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
       Set<String> urlList = new ForkJoinPool().invoke(new ParseUrl(url, new HashSet<>()));
       siteMap = new ArrayList<>(urlList);
    }

    public List<String> getSiteMap() {
        return siteMap;
    }
}
