package searchengine.repo;

import searchengine.models.Lemma;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    List<Lemma> findByLemma (String lemma);
    long count();
    long countBySiteId(int siteId);

}

