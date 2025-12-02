package com.example.angella.eventsplatform.exception;

//Исключение для ошибок сервера
public class ServerException extends RuntimeException {

    public ServerException(String message) {
        super(message);
    }

}