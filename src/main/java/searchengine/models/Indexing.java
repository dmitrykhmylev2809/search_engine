package searchengine.models;

import javax.persistence.*;

@Entity
@Table(name="index_table")
public class Indexing {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "page_id")
    private int pageId;
    @Column(name = "lemma_id")
    private int lemmaId;
    private float ranking;

    public Indexing() {
    }

    public Indexing(int pageId, int lemmaId, float ranking) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.ranking = ranking;
    }

    public int getPageId() {
        return pageId;
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public float getRank() {
        return ranking;
    }

    @Override
    public String toString() {
        return "Indexing{" +
                "pageId=" + pageId +
                ", lemmaId=" + lemmaId +
                ", ranking=" + ranking +
                '}';
    }
}
