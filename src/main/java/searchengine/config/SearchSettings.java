package searchengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "config")
public class SearchSettings {


    private String agent;
    private String referrer;

    private List<HashMap<String, String>> site;

    public SearchSettings() {
    }

    public String getAgent() {
        return agent;
    }

    public List<HashMap<String, String>> getSite() {
        return site;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public void setSite(List<HashMap<String, String>> site) {
        this.site = site;
    }


    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}
