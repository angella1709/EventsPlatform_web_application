package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.entity.Category;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Category with id {0} not found!", id)
                ));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Set<Category> upsertCategories(Set<Category> categories) {
        Set<String> eventCategories = categories.stream()
                .map(Category::getName).collect(Collectors.toSet());

        List<Category> existedCategories = categoryRepository.findAllByNameIn(eventCategories);
        Set<String> existedCategoryNames = existedCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        List<Category> categoriesForUpdate = categories.stream()
                .filter(it -> !existedCategoryNames.contains(it.getName()))
                .toList();

        // Логируем создание новых категорий
        if (!categoriesForUpdate.isEmpty()) {
            log.info("Creating new categories: {}",
                    categoriesForUpdate.stream().map(Category::getName).collect(Collectors.toList()));
        }

        return Stream.concat(existedCategories.stream(),
                        categoryRepository.saveAll(categoriesForUpdate).stream())
                .collect(Collectors.toSet());
    }

    public Long getTotalCategoriesCount() {
        return categoryRepository.count();
    }

}
