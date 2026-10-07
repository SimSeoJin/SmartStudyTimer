package com.example.smartstudytimer.member.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class homeController {

    @GetMapping
    public String getHello(){
        return "home";
    }
}

