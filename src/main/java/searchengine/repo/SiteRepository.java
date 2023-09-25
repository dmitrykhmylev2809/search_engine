package searchengine.repo;

import searchengine.models.Site;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Site findByUrl (String url);
    List<Site> findAll();
    Site findById(int siteId);
}
