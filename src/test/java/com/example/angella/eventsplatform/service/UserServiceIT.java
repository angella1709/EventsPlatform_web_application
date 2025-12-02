package com.example.angella.eventsplatform.service;

import com.example.angella.eventsplatform.ServiceIntegrationTest;
import com.example.angella.eventsplatform.entity.Role;
import com.example.angella.eventsplatform.entity.User;
import com.example.angella.eventsplatform.exception.EntityNotFoundException;
import com.example.angella.eventsplatform.exception.RegisterUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceIT extends ServiceIntegrationTest {

    @Autowired
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя для использования в нескольких тестах
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userService.registerUser(testUser);
    }

    // Тест проверяет успешную регистрацию пользователя с шифрованием пароля
    @Test
    void registerUser_ShouldSaveUserWithEncryptedPassword() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("rawpassword");

        User savedUser = userService.registerUser(newUser);

        // Проверяем, что пользователь сохранен с ID
        assertNotNull(savedUser.getId());
        // Проверяем, что пароль был зашифрован (не равен исходному)
        assertNotEquals("rawpassword", savedUser.getPassword());
        // Проверяем, что пользователю назначена роль ROLE_USER по умолчанию
        assertEquals(Set.of(Role.ROLE_USER), savedUser.getRoles());
        // Проверяем, что установлены даты создания и обновления
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }

    // Тест проверяет, что при регистрации с существующим username выбрасывается исключение
    @Test
    void registerUser_ShouldThrowWhenUsernameExists() {
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // Используем уже существующий username
        duplicateUser.setEmail("newemail@example.com");
        duplicateUser.setPassword("password");

        assertThrows(RegisterUserException.class, () -> userService.registerUser(duplicateUser));
    }

    // Тест проверяет, что при регистрации с существующим email выбрасывается исключение
    @Test
    void registerUser_ShouldThrowWhenEmailExists() {
        User duplicateUser = new User();
        duplicateUser.setUsername("newusername");
        duplicateUser.setEmail("test@example.com"); // Используем уже существующий email
        duplicateUser.setPassword("password");

        assertThrows(RegisterUserException.class, () -> userService.registerUser(duplicateUser));
    }

    // Тест проверяет успешный поиск пользователя по username
    @Test
    void findByUsername_ShouldReturnUser() {
        User foundUser = userService.findByUsername("testuser");

        // Проверяем, что пользователь найден
        assertNotNull(foundUser);
        // Проверяем корректность email найденного пользователя
        assertEquals("test@example.com", foundUser.getEmail());
        // Проверяем, что у пользователя есть ожидаемые роли
        assertEquals(Set.of(Role.ROLE_USER), foundUser.getRoles());
    }

    // Тест проверяет, что при поиске несуществующего пользователя выбрасывается исключение
    @Test
    void findByUsername_ShouldThrowWhenUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userService.findByUsername("nonexisting"));
    }

    // Тест проверяет успешный поиск пользователя по ID
    @Test
    void findById_ShouldReturnUser() {
        User foundUser = userService.findById(testUser.getId());

        // Проверяем, что пользователь найден
        assertNotNull(foundUser);
        // Проверяем корректность username найденного пользователя
        assertEquals("testuser", foundUser.getUsername());
        // Проверяем, что у пользователя есть ожидаемые роли
        assertEquals(Set.of(Role.ROLE_USER), foundUser.getRoles());
    }

    // Тест проверяет, что при поиске по несуществующему ID выбрасывается исключение
    @Test
    void findById_ShouldThrowWhenUserNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userService.findById(9999L));
    }

    // Тест проверяет, что новому пользователю по умолчанию назначается роль ROLE_USER
    @Test
    void registerUser_ShouldAssignUserRoleByDefault() {
        User newUser = new User();
        newUser.setUsername("defaultroleuser");
        newUser.setEmail("default@example.com");
        newUser.setPassword("password");

        User savedUser = userService.registerUser(newUser);

        // Проверяем, что пользователю назначена только роль ROLE_USER
        assertEquals(Set.of(Role.ROLE_USER), savedUser.getRoles());
    }
}