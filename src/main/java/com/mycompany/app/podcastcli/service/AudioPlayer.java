package com.mycompany.app.podcastcli.service;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPlayer {

    private Player jlPlayer;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private Thread playerThread;
    private Thread inputListenerThread;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_ORANGE = "\u001B[38;5;208m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final Object consoleLock = new Object();

    public void play(String audioUrl, String title) {
        if (!isPlaying.compareAndSet(false, true)) {
            System.out.println(ANSI_YELLOW + "Audio is already playing. Stopping previous playback first." + ANSI_RESET);
            stop();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            if (!isPlaying.compareAndSet(false, true)) {
                System.err.println(ANSI_RED + "Failed to acquire playing lock after stopping. Please try again." + ANSI_RESET);
                return;
            }
        }

        playerThread = new Thread(() -> {
            InputStream inputStream = null;
            try {
                synchronized (consoleLock) {
                    System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + "▶ Attempting JLayer playback: "
                            + ANSI_RESET + title);
                    System.out.println(ANSI_BOLD + ANSI_ORANGE + "Source: "
                            + ANSI_RESET + audioUrl + "\n");
                }

                URL url = new URL(audioUrl);
                inputStream = new BufferedInputStream(url.openStream());
                jlPlayer = new Player(inputStream);

                synchronized (consoleLock) {
                    System.out.println(ANSI_GREEN + "Streaming audio using JLayer (MP3)..." + ANSI_RESET);
                }
                showPlayerControls();
                startInputListener();
                startProgressThread();

                jlPlayer.play();

                if (isPlaying.get()) {
                    synchronized (consoleLock) {
                        System.out.println("\n" + ANSI_GREEN + "Playback finished naturally." + ANSI_RESET);
                    }
                }

            } catch (JavaLayerException e) {
                System.err.println(ANSI_RED + "JLayer Error: Could not play audio." + ANSI_RESET);
                System.err.println("Details: " + e.getMessage());
            } catch (IOException e) {
                System.err.println(ANSI_RED + "Error opening or reading audio stream: " + e.getMessage() + ANSI_RESET);
            } catch (Exception e) {
                System.err.println(ANSI_RED + "An unexpected error occurred during playback: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
            } finally {
                closeJLayerPlayer();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) { }
                }
                isPlaying.set(false);
                if (inputListenerThread != null && inputListenerThread.isAlive()) {
                    inputListenerThread.interrupt();
                }
            }
        });
        playerThread.setName("JLayerAudioPlayerThread");
        playerThread.start();
    }

    private synchronized void closeJLayerPlayer() {
        if (jlPlayer != null) {
            jlPlayer.close();
            jlPlayer = null;
        }
    }

    private void showPlayerControls() {
        synchronized (consoleLock) {
            System.out.println("\n" + ANSI_BOLD + ANSI_ORANGE + "Controls:" + ANSI_RESET);
            System.out.println(ANSI_RED + "Press 'q' + Enter to stop playback" + ANSI_RESET);
        }
    }

    private void startInputListener() {
        if (inputListenerThread != null && inputListenerThread.isAlive()) {
            inputListenerThread.interrupt();
        }
        inputListenerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (isPlaying.get() && !Thread.currentThread().isInterrupted()) {
                    synchronized (consoleLock) {
                        System.out.print("> ");
                    }
                    if (reader.ready()) {
                        String lineInput = reader.readLine();
                        synchronized (consoleLock) {
                            System.out.print("\r");
                        }
                        if (lineInput != null && lineInput.trim().equalsIgnoreCase("q")) {
                            stop();
                            break;
                        }
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                if (isPlaying.get()) {
                    System.err.println(ANSI_RED + "Error reading console input: " + e.getMessage() + ANSI_RESET);
                }
            }
        });
        inputListenerThread.setName("AudioInputListenerThread");
        inputListenerThread.setDaemon(true);
        inputListenerThread.start();
    }

    private void startProgressThread() {
        Thread progressThread = new Thread(() -> {
            String[] spinner = new String[] {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
            int spinnerIndex = 0;
            while (isPlaying.get()) {
                long position;
                synchronized (this) {
                    position = (jlPlayer != null) ? jlPlayer.getPosition() : 0;
                }
                long minutes = (position / 1000) / 60;
                long seconds = (position / 1000) % 60;
                String time = String.format("%d:%02d", minutes, seconds);
                String spinnerChar = spinner[spinnerIndex % spinner.length];
                spinnerIndex++;
                synchronized (consoleLock) {
                    System.out.print("\r" + ANSI_ORANGE + spinnerChar + " " + ANSI_RESET + "Elapsed: " + time + " ");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            synchronized (consoleLock) {
                System.out.print("\r\033[K");
            }
        });
        progressThread.setDaemon(true);
        progressThread.setName("ProgressThread");
        progressThread.start();
    }

    public void stop() {
        if (isPlaying.compareAndSet(true, false)) {
            synchronized (consoleLock) {
                System.out.println("\n" + ANSI_BOLD + ANSI_RED + "Stopping playback..." + ANSI_RESET);
            }
            closeJLayerPlayer();
            if (inputListenerThread != null && inputListenerThread.isAlive()) {
                inputListenerThread.interrupt();
            }
            synchronized (consoleLock) {
                System.out.println(ANSI_BOLD + ANSI_RED + "Playback stop requested." + ANSI_RESET);
            }
        } else {
            synchronized (consoleLock) {
                System.out.println(ANSI_YELLOW + "Playback is not currently active." + ANSI_RESET);
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying.get();
    }
}