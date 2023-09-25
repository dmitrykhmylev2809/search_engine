package searchengine.repo;

import searchengine.models.Indexing;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Indexing, Integer> {
    Indexing findByLemmaIdAndPageId (int lemmaId, int pageId);
    List<Indexing> getAllIndexingByLemmaId(int lemmaId);
    List<Indexing> getAllIndexingByPageId(int pageId);

}




