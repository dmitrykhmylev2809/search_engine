package searchengine.dao.impl;

import org.springframework.stereotype.Component;
import searchengine.models.Field;
import searchengine.repo.FieldRepository;
import searchengine.dao.FieldRepositoryDao;

import java.util.ArrayList;
import java.util.List;

@Component
public class FieldRepoImpl implements FieldRepositoryDao {

    private final FieldRepository fieldRepository;

    public FieldRepoImpl(FieldRepository fieldRepository) {
        this.fieldRepository = fieldRepository;
    }

    @Override
    public Field getFieldByName(String fieldName) {
        return fieldRepository.findByName(fieldName);
    }
    @Override
    public synchronized void save(Field field) {
        fieldRepository.save(field);
    }

    @Override
    public List<Field> getAllField() {
        List<Field> list = new ArrayList<>();
        Iterable<Field> iterable = fieldRepository.findAll();
        iterable.forEach(list::add);
        return list;
    }
}
