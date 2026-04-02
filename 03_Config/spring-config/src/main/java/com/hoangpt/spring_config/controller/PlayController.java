package com.hoangpt.spring_config.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/play")
public class PlayController {
    @Value("${profile.name}")
    private String name;

    @Value("${profile.age}")
    private int age;

    @GetMapping("/profile")
    public String getProfile() {
        return name + " - " + age;
    }
}
