package com.isupov.homework.linkShorter.models;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class User {
    private final String uuid;
    private final Map<String, Link> links;

    public User(String uuid) {
        this.uuid = uuid;
        this.links = new HashMap<>();
    }
}
