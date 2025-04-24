package com.mycompany.app.podcastcli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PodcastCliApplication {

    public static void main(String[] args) {

        System.setProperty("spring.main.web-application-type", "none");
        SpringApplication.run(PodcastCliApplication.class, args);
    }

}
