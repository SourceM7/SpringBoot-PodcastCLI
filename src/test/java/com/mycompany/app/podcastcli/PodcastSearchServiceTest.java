package com.mycompany.app.podcastcli;

import com.mycompany.app.podcastcli.model.ItunesSearchResponse;
import com.mycompany.app.podcastcli.model.Result;
import com.mycompany.app.podcastcli.service.ItunesSearchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PodcastSearchServiceTest {

    @Test
    public void testSearch() {
        ItunesSearchService service = new ItunesSearchService();
        ItunesSearchResponse response = service.search("ثمانية");


        assertNotNull(response);
        assertNotNull(response.getResults());


        for (Result result : response.getResults()) {
            System.out.println(result.getArtistName());
        }


    }
}
