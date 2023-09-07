package searchengine.dao.impl;

import org.springframework.stereotype.Component;
import searchengine.models.Indexing;
import searchengine.repo.IndexRepository;
import searchengine.dao.IndexRepositoryDao;

import java.util.List;

@Component
public class IndexRepoImpl implements IndexRepositoryDao {

    private final IndexRepository indexRepository;

    public IndexRepoImpl(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    @Override
    public List<Indexing> getAllIndexingByLemmaId(int lemmaId) {
        return indexRepository.findByLemmaId(lemmaId);
    }

    @Override
    public List<Indexing> getAllIndexingByPageId(int pageId) {
        return indexRepository.findByPageId(pageId);
    }

    @Override
    public synchronized void deleteAllIndexing(List<Indexing> indexingList){
        indexRepository.deleteAll(indexingList);
    }

    @Override
    public Indexing getIndexing(int lemmaId, int pageId) {
        Indexing indexing = null;
        try{
            indexing = indexRepository.findByLemmaIdAndPageId(lemmaId, pageId);
        } catch (Exception e) {
            System.out.println("lemmaId: " + lemmaId + " + pageId: " + pageId + " not unique");
            e.printStackTrace();
        }
        return indexing;
    }

    @Override
    public synchronized void save(Indexing indexing) {
        indexRepository.save(indexing);
    }

}
