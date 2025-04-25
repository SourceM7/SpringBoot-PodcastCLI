package com.mycompany.app.podcastcli.command;

import com.mycompany.app.podcastcli.model.ItunesSearchResponse;
import com.mycompany.app.podcastcli.model.Result;
import com.mycompany.app.podcastcli.service.AudioPlayer;
import com.mycompany.app.podcastcli.service.FeedSaxParser;
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

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_ORANGE = "\u001B[38;5;208m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_DIM = "\u001B[2m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_WHITE = "\u001B[97m";

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

        System.out.println(ANSI_BOLD + ANSI_ORANGE + " Welcome to Podcast CLI! " + ANSI_RESET);
        System.out.println(ANSI_DIM + ANSI_WHITE + "   Type 'q' in the main menu to exit interactive mode." + ANSI_RESET);


        while (stateManager.getCurrentState() != ApplicationState.EXIT) {
            switch (stateManager.getCurrentState()) {
                case MAIN_MENU:
                    handleMainMenu();
                    break;
                case SEARCH:
                    handleSearch();
                    break;
                case SHOWING_SEARCH_RESULTS:
                    break;
                case BOOKMARK:
                    handleBookmark();
                    break;
                default:
                    System.err.println(ANSI_RED + "Error: Unknown state " + stateManager.getCurrentState() + ANSI_RESET);
                    stateManager.changeState(ApplicationState.MAIN_MENU);
                    break;
            }
        }
        System.out.println(ANSI_ORANGE + " Exiting interactive mode. Goodbye!" + ANSI_RESET);

    }




    private void handleMainMenu() {
        System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + " === Main Menu === " + ANSI_RESET);
        System.out.println(ANSI_ORANGE + "  1." + ANSI_RESET + " Search for a Podcast");
        System.out.println(ANSI_ORANGE + "  2." + ANSI_RESET + " Access your Bookmarks");
        System.out.println(ANSI_ORANGE + "  q." + ANSI_RESET + " Quit Interactive Mode");
        System.out.println(ANSI_ORANGE + "=====================" + ANSI_RESET);

        String input = lineReader.readLine(ANSI_BOLD + ANSI_ORANGE + "Choose an option > " + ANSI_RESET);

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
                System.out.println(ANSI_RED + " Invalid choice, please try again." + ANSI_RESET);
                break;
        }
    }


    private void handleSearch() {
        System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + " === Search Podcast === " + ANSI_RESET);
        String podcastName = lineReader.readLine(ANSI_ORANGE + "Podcast Title (or type 'back') > " + ANSI_RESET);

        if ("back".equalsIgnoreCase(podcastName.trim())) {
            stateManager.changeState(ApplicationState.MAIN_MENU);
            return;
        }

        System.out.println(ANSI_DIM + ANSI_WHITE + "Searching for: '" + ANSI_BOLD + podcastName + ANSI_DIM + "'..." + ANSI_RESET);

        try {
            ItunesSearchResponse searchResponse = itunesSearchService.search(podcastName);

            if(searchResponse != null && searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                System.out.println(ANSI_ORANGE + "Found " + ANSI_BOLD + searchResponse.getResults().size() + ANSI_RESET + ANSI_ORANGE + " results:" + ANSI_RESET);
                List<Result> results = searchResponse.getResults();
                System.out.println(ANSI_ORANGE + "----------------------------------------" + ANSI_RESET);
                for (int i =0; i < results.size(); i++) {
                    Result result = results.get(i);
                    System.out.printf(ANSI_BOLD + ANSI_ORANGE + "%d. " + ANSI_RESET + ANSI_BOLD + "%s" + ANSI_RESET + " - %s%n",
                            i + 1,
                            result.getArtistName() != null ? result.getArtistName() : "Unknown Artist",
                            result.getCollectionName() != null ? result.getCollectionName() : "Unknown Collection");
                }
                System.out.println(ANSI_ORANGE + "----------------------------------------" + ANSI_RESET);
                stateManager.changeState(ApplicationState.SHOWING_SEARCH_RESULTS);

                Result chosenResult = null;
                while(chosenResult == null && stateManager.getCurrentState() == ApplicationState.SHOWING_SEARCH_RESULTS) {
                    String choiceInput = lineReader.readLine(ANSI_BOLD + ANSI_ORANGE + "Enter result number to view details (1-" + results.size() + ") or 'back' > " + ANSI_RESET);

                    if("back".equalsIgnoreCase(choiceInput.trim())) {
                        stateManager.changeState(ApplicationState.MAIN_MENU);
                        return;
                    }

                    if(choiceInput == null || "".equals(choiceInput.trim())) {
                        System.out.println(ANSI_RED + "Please enter a number or 'back'." + ANSI_RESET);
                        continue;
                    }
                    try{
                        int choiceN= Integer.parseInt(choiceInput.trim());
                        if(choiceN>=1 && choiceN<=results.size()) {
                            chosenResult = results.get(choiceN-1);
                            System.out.println(ANSI_ORANGE + "▶ You selected: " + ANSI_BOLD + chosenResult.getArtistName() + " - " + chosenResult.getCollectionName() + ANSI_RESET);
                            handleShowSearchResults(chosenResult.getFeedUrl());
                            stateManager.changeState(ApplicationState.MAIN_MENU);
                            return;

                        } else {
                            System.out.println(ANSI_RED + "Invalid number. Please choose between 1 and " + results.size() + "." + ANSI_RESET);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(ANSI_RED + "Invalid input. Please enter a number or 'back'." + ANSI_RESET);
                    }
                }
            } else {
                System.out.println(ANSI_ORANGE + " No results found for '" + ANSI_BOLD + podcastName + ANSI_RESET + ANSI_ORANGE + "'." + ANSI_RESET);
            }
        } catch (Exception e){
            System.err.println(ANSI_RED + "Error searching for podcast: " + e.getMessage() + ANSI_RESET);
        }
        if (stateManager.getCurrentState() != ApplicationState.EXIT) {
            stateManager.changeState(ApplicationState.MAIN_MENU);
        }
    }

    private void handleShowSearchResults(String feedUrl) {
        List<FeedSaxParser.Episode> episodes = new FeedSaxParser().parseFeed(feedUrl);

        if (episodes.isEmpty()) {
            System.out.println(ANSI_RED + "No episodes found in this feed." + ANSI_RESET);
            return;
        }

        System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + " === Episode List === " + ANSI_RESET);
        for (int i = 0; i < episodes.size(); i++) {
            FeedSaxParser.Episode episode = episodes.get(i);
            System.out.printf(ANSI_BOLD + ANSI_ORANGE + "%d. " + ANSI_RESET + ANSI_BOLD + "%s" + ANSI_RESET + "%n",
                    i + 1,
                    episode.getTitle());
        }

        System.out.println(ANSI_ORANGE + "----------------------------------------" + ANSI_RESET);

        boolean stayInEpisodeMenu = true;
        AudioPlayer audioPlayer = new AudioPlayer();

        while (stayInEpisodeMenu) {
            String input = lineReader.readLine(ANSI_BOLD + ANSI_ORANGE + "Enter episode number to play or 'back' > " + ANSI_RESET);

            if ("back".equalsIgnoreCase(input.trim())) {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.stop();
                }
                stayInEpisodeMenu = false;
                continue;
            }

            try {
                int choice = Integer.parseInt(input.trim());
                if (choice >= 1 && choice <= episodes.size()) {
                    FeedSaxParser.Episode selectedEpisode = episodes.get(choice - 1);
                    String mp3Url = selectedEpisode.getAudioUrl();

                    if (mp3Url == null || mp3Url.isEmpty()) {
                        System.out.println(ANSI_RED + "No audio URL found for this episode." + ANSI_RESET);
                        continue;
                    }

                    if (audioPlayer.isPlaying()) {
                        audioPlayer.stop();
                    }

                    System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + " === Selected Episode === " + ANSI_RESET);
                    System.out.println(ANSI_BOLD + "Title: " + ANSI_RESET + selectedEpisode.getTitle());

                    String description = selectedEpisode.getDescription();
                    if (description.length() > 200) {
                        description = description.substring(0, 197) + "...";
                    }
                    System.out.println(ANSI_BOLD + "Description: " + ANSI_RESET + description);

                    String playConfirm = lineReader.readLine(ANSI_BOLD + ANSI_ORANGE + "Play this episode? (y/n) > " + ANSI_RESET);
                    if ("y".equalsIgnoreCase(playConfirm.trim())) {
                        System.out.println(ANSI_ORANGE + "Loading audio stream..." + ANSI_RESET);
                        audioPlayer.play(mp3Url, selectedEpisode.getTitle());
                    }
                } else {
                    System.out.println(ANSI_RED + "Invalid number. Please choose between 1 and " + episodes.size() + "." + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Invalid input. Please enter a number or 'back'." + ANSI_RESET);
            }
        }
    }


    private void handleBookmark() {
        System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + " === Bookmarks === " + ANSI_RESET);
        System.out.println(ANSI_DIM + ANSI_WHITE + "(Bookmark functionality not yet implemented)" + ANSI_RESET);
        String input = lineReader.readLine(ANSI_ORANGE + "Type 'back' to return to main menu > " + ANSI_RESET);
        if ("back".equalsIgnoreCase(input.trim())) {
            stateManager.changeState(ApplicationState.MAIN_MENU);
        } else {
            stateManager.changeState(ApplicationState.MAIN_MENU);
        }
    }
}