package searchengine.repo;

import searchengine.models.Field;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends CrudRepository<Field, Integer> {

    Field getFieldByName(String fieldName);

}