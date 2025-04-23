package com.mycompany.app.podcastcli.service;

import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ItunesSearchService {
    protected String URL = "https://itunes.apple.com";


    public String search(String query) {
        WebClient webClient = WebClient.create(URL);

        Mono<String> res = webClient.get().uri("/search?q=" + query).retrieve().bodyToMono(String.class);

        return res.block();
    }
}
