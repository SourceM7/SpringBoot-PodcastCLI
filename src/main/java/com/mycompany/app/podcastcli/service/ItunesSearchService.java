package com.mycompany.app.podcastcli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.app.podcastcli.model.ItunesSearchResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;


@Service
public class ItunesSearchService {
    private final WebClient webClient;
    protected String URL = "https://itunes.apple.com";

    public ItunesSearchService() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    configurer.customCodecs().register(
                            new Jackson2JsonDecoder(objectMapper, MediaType.parseMediaType("text/javascript")));
                })
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(URL)
                .exchangeStrategies(strategies)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ItunesSearchResponse search(String query) {
        WebClient webClient = WebClient.create(URL);

        ItunesSearchResponse response=  this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("term", query)
                        .queryParam("media", "podcast")
                        .queryParam("entity", "podcast")
                        .queryParam("limit", 10)
                        .build())
                .retrieve()
                .bodyToMono(ItunesSearchResponse.class)
                .block();

//        response.getResults().forEach(result -> {
//            new FeedSaxParser().parseFeed(result.getFeedUrl());
//        });

        return response;
    }
}
