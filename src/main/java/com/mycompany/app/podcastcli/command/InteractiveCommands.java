package com.mycompany.app.podcastcli.command;

import com.mycompany.app.podcastcli.model.ItunesSearchResponse;
import com.mycompany.app.podcastcli.model.Result;
import com.mycompany.app.podcastcli.service.ItunesSearchService;
import com.mycompany.app.podcastcli.state.ApplicationState;
import com.mycompany.app.podcastcli.state.ApplicationStateManager;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
public class InteractiveCommands {

    @Autowired
    private ItunesSearchService itunesSearchService;

    @Autowired
    private ApplicationStateManager stateManager;

    @Autowired @Lazy
    private LineReader lineReader;


    @ShellMethod(key = {"start", "run"}, value = "Start the interactive podcast CLI.")
    public void start() {

        if (stateManager.getCurrentState() == ApplicationState.EXIT) {
            stateManager.changeState(ApplicationState.MAIN_MENU);
        }

        System.out.println("Welcome to Podcast CLI! Type 'q' in the main menu to exit interactive mode.");

        while (stateManager.getCurrentState() != ApplicationState.EXIT) {
            switch (stateManager.getCurrentState()) {
                case MAIN_MENU:
                    handleMainMenu();
                    break;
                case SEARCH:
                    handleSearch();
                    break;
                case SHOWING_SEARCH_RESULTS:
                    handleShowSearchResults();
                    break;
                case BOOKMARK:
                    handleBookmark();
                    break;
                default:
                    System.err.println("Error: Unknown state " + stateManager.getCurrentState());
                    stateManager.changeState(ApplicationState.MAIN_MENU);
                    break;
            }
        }
        System.out.println("Exiting interactive mode.");

    }




    private void handleMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Search for a Podcast");
        System.out.println("2. Access your Bookmarks");
        System.out.println("q. Quit Interactive Mode");
        System.out.println("-----------------------");

        String input = lineReader.readLine("Enter Choice: ");

        switch (input.trim().toLowerCase()) {
            case "1":
                stateManager.changeState(ApplicationState.SEARCH);
                break;
            case "2":
                stateManager.changeState(ApplicationState.BOOKMARK);
                break;
            case "q":
                stateManager.changeState(ApplicationState.EXIT);
                break;
            default:
                System.out.println("Invalid choice, please try again.");
                break;
        }
    }


    private void handleSearch() {
        System.out.println("\n--- Search Podcast ---");
        String podcastName = lineReader.readLine("Podcast Title (or type 'back'): ");

        if ("back".equalsIgnoreCase(podcastName.trim())) {
            stateManager.changeState(ApplicationState.MAIN_MENU);
            return;
        }

        System.out.println("Searching for: '" + podcastName + "'...");

        try {
            ItunesSearchResponse searchResponse = itunesSearchService.search(podcastName);

            if(searchResponse != null && searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                System.out.println("Found " + searchResponse.getResults().size() + " results.");
                List<Result> results = searchResponse.getResults();
                for (int i =0; i < results.size(); i++) {
                    Result result = results.get(i);
                    System.out.printf("%d. %s - %s%n",
                            i + 1,
                            result.getArtistName(),
                            result.getCollectionName());
                    System.out.println("   Feed: " + result.getFeedUrl());
                    stateManager.changeState(ApplicationState.SHOWING_SEARCH_RESULTS);

                }
            } else {
                System.out.println("No results found.");
            }
            } catch (Exception e){
            System.out.println("Error searching for: " + e.getMessage());
        }

        stateManager.changeState(ApplicationState.MAIN_MENU);
    }

    private void handleShowSearchResults() {
    }


    private void handleBookmark() {

    }
}