package com.studymate.global.exception;

public class ForbiddenException extends RuntimeException{

    public ForbiddenException(String message){
        super(message);
    }
}
