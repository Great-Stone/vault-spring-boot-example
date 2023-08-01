package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {
    private final AppConfiguration appConfiguration;
    @PostConstruct
    public void readConfigs() {
        log.info("Reading configuration {} - {}", appConfiguration.getToken(), appConfiguration.getUsername());
    }
}