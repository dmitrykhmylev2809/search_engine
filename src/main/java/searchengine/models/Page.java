package searchengine.models;

import javax.persistence.*;

@Entity
@Table(name="page",
        indexes = {@Index(name = "Path_INDEX", columnList = "path")})

public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String path;
    private int code;

    @Column(columnDefinition = "LONGTEXT")
    private String content;
    @Column(name = "site_id")
    private int siteId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }
}

