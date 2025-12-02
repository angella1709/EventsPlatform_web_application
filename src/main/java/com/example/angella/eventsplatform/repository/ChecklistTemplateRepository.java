package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.ChecklistTemplate;
import com.example.angella.eventsplatform.entity.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

    List<ChecklistTemplate> findByCategory(TemplateCategory category);

    @Query("SELECT t FROM ChecklistTemplate t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ChecklistTemplate> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT t FROM ChecklistTemplate t JOIN t.items i GROUP BY t HAVING COUNT(i) > 0")
    List<ChecklistTemplate> findTemplatesWithItems();
}