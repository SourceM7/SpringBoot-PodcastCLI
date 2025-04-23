package com.mycompany.app.podcastcli.command;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class MainCommand {



    @ShellMethod(key = "hello", value = "Say hello!")
    public String hello(String name) {
        return "Hello!" + name;
    }
}
