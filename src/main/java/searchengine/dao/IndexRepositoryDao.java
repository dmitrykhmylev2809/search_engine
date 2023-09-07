package searchengine.dao;

import searchengine.models.Indexing;

import java.util.List;

public interface IndexRepositoryDao {
    List<Indexing> getAllIndexingByLemmaId(int lemmaId);
    List<Indexing> getAllIndexingByPageId(int pageId);
    void deleteAllIndexing(List<Indexing> indexingList);
    Indexing getIndexing (int lemmaId, int pageId);
    void save(Indexing indexing);

}
