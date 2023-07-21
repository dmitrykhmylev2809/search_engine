package searchengine.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.Field;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldRepository extends CrudRepository<Field, Integer> {
    Field findByName(String name);
}