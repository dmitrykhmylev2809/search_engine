package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;


public class ParseUrl extends RecursiveTask<Set<String>> {

    private String url;
    private Set<String> allUrls;
    private Set<String> visitedUrls;

    public volatile boolean  isStopped;


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
            Connection.Response response = Jsoup.connect(url).ignoreContentType(true).timeout(25000).execute();
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                Document doc = response.parse();
                Elements links = doc.select("a[href~=^[^#]+$]");

                Set<String> childUrls = links.stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(this::isSuitableLink)
                        .collect(Collectors.toSet());

                if (!isStopped) {
                    Set<ParseUrl> subTasks = childUrls.stream()
                            .filter(childUrl -> !allUrls.contains(childUrl))
                            .map(childUrl -> new ParseUrl(childUrl, allUrls))
                            .collect(Collectors.toSet());

                    invokeAll(subTasks);

                    for (ParseUrl task : subTasks) {
                        allUrls.addAll(task.join());
                    }
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
        String mediaRegex =
                "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|pdf|php|zip)$|[#]";
        return link.startsWith(url) && !link.matches(mediaRegex);
    }
}
