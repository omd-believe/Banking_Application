package com.banking.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HelloUser {

    @GetMapping("/hello")
    public String hello(){
        return "Hello User! " +
                "Welcome to our Bank.";
    }
}
