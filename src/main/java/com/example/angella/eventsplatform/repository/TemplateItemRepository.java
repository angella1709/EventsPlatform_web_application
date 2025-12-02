package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.TemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateItemRepository extends JpaRepository<TemplateItem, Long> {

    List<TemplateItem> findByTemplateId(Long templateId);

    void deleteByTemplateId(Long templateId);
}