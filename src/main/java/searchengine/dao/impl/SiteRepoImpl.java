package searchengine.dao.impl;

import org.springframework.stereotype.Component;
import searchengine.models.Site;
import searchengine.repo.SiteRepository;
import searchengine.dao.SiteRepositoryDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SiteRepoImpl implements SiteRepositoryDao {

    private final SiteRepository siteRepository;

    public SiteRepoImpl(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public Site getSite(String url) {
        return siteRepository.findByUrl(url);
    }
    public Site getSite(int siteId) {
        Optional<Site> optional = siteRepository.findById(siteId);
        Site site = null;
        if(optional.isPresent()){
            site = optional.get();
        }
        return site;
    }

    @Override
    public synchronized void save(Site site) {
        siteRepository.save(site);
    }

    @Override
    public long siteCount(){
        return siteRepository.count();
    }

    @Override
    public List<Site> getAllSites() {
        List<Site> siteList = new ArrayList<>();
        Iterable<Site> it = siteRepository.findAll();
        it.forEach(siteList::add);
        return siteList;
    }
}
