package searchengine.repo;

import searchengine.models.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    Page findByPath (String path);
    Optional<Page> findByIdAndSiteId (int id, int siteId);
    long count();
    long countBySiteId(int siteId);
}