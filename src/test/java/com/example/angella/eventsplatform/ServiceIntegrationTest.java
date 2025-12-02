package com.example.angella.eventsplatform;

import com.example.angella.eventsplatform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class ServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void clearDatabase(
            @Autowired UserRepository userRepository,
            @Autowired EventRepository eventRepository,
            @Autowired CategoryRepository categoryRepository,
            @Autowired ChatMessageRepository chatMessageRepository,
            @Autowired CommentRepository commentRepository,
            @Autowired TaskRepository taskRepository
    ) {
        // Очистка в правильном порядке (сначала дочерние сущности)
        taskRepository.deleteAll();
        chatMessageRepository.deleteAll();
        commentRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}