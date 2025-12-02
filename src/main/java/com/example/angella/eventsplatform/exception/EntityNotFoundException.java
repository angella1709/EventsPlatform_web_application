package com.example.angella.eventsplatform.exception;

//Исключение для случаев, когда сущность не найдена
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}