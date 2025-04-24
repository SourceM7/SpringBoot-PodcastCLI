package com.mycompany.app.podcastcli.state;


import org.springframework.shell.Availability;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStateManager {

    private ApplicationState currentState = ApplicationState.MAIN_MENU;

    public ApplicationState getCurrentState() {
        return currentState;
    }

    public void changeState(ApplicationState newState) {
        this.currentState = newState;
    }


    public Availability isMainMenuState() {
        boolean isMainMenu = currentState == ApplicationState.MAIN_MENU;
        return isMainMenu
                ? Availability.available()
                : Availability.unavailable("you are not in the main menu. Current state: " + currentState);
    }
}
