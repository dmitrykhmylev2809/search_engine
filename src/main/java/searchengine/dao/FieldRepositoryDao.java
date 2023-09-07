package searchengine.dao;

import searchengine.models.Field;

import java.util.List;

public interface FieldRepositoryDao {
    Field getFieldByName(String fieldName);
    void save(Field field);
    List<Field> getAllField();


}
