package com.banking.account.exception;

public class FraudAlertException extends RuntimeException{
    public FraudAlertException(String message){
        super(message);
    }
}
