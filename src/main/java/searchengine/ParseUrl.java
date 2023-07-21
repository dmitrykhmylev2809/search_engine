package searchengine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.models.Page;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;


//public class ParseUrl extends RecursiveTask<String> {
//    public final static List<String> urlList = new Vector<>();
//
//    private final static Log log = LogFactory.getLog(ParseUrl.class);
//    private final String url;
//    private final boolean isInterrupted;
//
//    private final SearchSettings searchSettings;
//
//    public volatile boolean  isStopped;
//
//    public ParseUrl(String url, boolean isInterrupted, SearchSettings searchSettings) {
//        this.url = url;
//        this.isInterrupted = isInterrupted;
//        this.searchSettings = searchSettings;
//    }
//
//    @Override
//    protected String compute() {
//        if(isInterrupted){
//            return "";
//        }
//        StringBuilder result = new StringBuilder();
//        result.append(url);
//        try {
//            Thread.sleep(200);
//            Document doc = getDocumentByUrl(url);
//            Elements rootElements = doc.select("a");
//
//            List<ParseUrl> linkGrabers = new ArrayList<>();
//            rootElements.forEach(element -> {
//                String link = element.attr("abs:href");
//                if (link.startsWith(element.baseUri())
//                        && !link.equals(element.baseUri())
//                        && !link.contains("#")
//                        && !link.contains(".pdf")
//                        && !urlList.contains(link)
//                        && !isStopped
//
//                ) {
//                    urlList.add(link);
//                    ParseUrl linkGraber = new ParseUrl(link, false, searchSettings);
//                    linkGraber.fork();
//                    linkGrabers.add(linkGraber);
//                }
//            });
//
//            for (ParseUrl lg : linkGrabers) {
//                String text = lg.join();
//                if (!text.equals("")) {
//                    result.append("\n");
//                    result.append(text);
//                }
//            }
//        } catch (IOException | InterruptedException e) {
//            log.warn("Ошибка парсинга сайта: " + url);
//        }
//        return result.toString();
//    }
//
//    protected Document getDocumentByUrl (String url) throws InterruptedException, IOException {
//        Thread.sleep(200);
//        return Jsoup.connect(url)
//                .maxBodySize(0)
//                .userAgent(searchSettings.getAgent())
//                .referrer(searchSettings.getReferrer())
//                .get();
//    }
//}

public class ParseUrl extends RecursiveTask<Set<String>> {

    private String url;
    private Set<String> allUrls;
    private Set<String> visitedUrls;

    public ParseUrl(String url, Set<String> allUrls) {
        this.url = url;
        this.allUrls = allUrls;
        this.visitedUrls = new HashSet<>();
        allUrls.add(url);
        visitedUrls.add(url);
    }

    @Override
    protected Set<String> compute() {
        try {
            Connection.Response response = Jsoup.connect(url).ignoreContentType(true).execute();
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                Document doc = response.parse();
                Elements links = doc.select("a[href]");

                Set<String> childUrls = links.stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(this::isSuitableLink)
                        .collect(Collectors.toSet());

                childUrls.forEach(System.out::println);

                Set<ParseUrl> subTasks = childUrls.stream()
                        .filter(childUrl -> !allUrls.contains(childUrl)) // Фильтрация повторяющихся URL-адресов
                        .map(childUrl -> new ParseUrl(childUrl, allUrls))
                        .collect(Collectors.toSet());

                invokeAll(subTasks);

                for (ParseUrl task : subTasks) {
                    allUrls.addAll(task.join());
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Превышено время ожидания чтения: " + url);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return allUrls;
    }

    private boolean isSuitableLink(String link) {
        return link.startsWith(url) && !link.contains("#")
                && !link.endsWith(".pdf")
                && !link.contains("instagram.com")
                && !link.contains("vk.com");
    }
}
