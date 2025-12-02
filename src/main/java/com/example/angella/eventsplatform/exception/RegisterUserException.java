package com.example.angella.eventsplatform.exception;

//Исключение, возникающее при ошибках регистрации пользователя
public class RegisterUserException extends RuntimeException {

    public RegisterUserException(String message) {
        super(message);
    }

}