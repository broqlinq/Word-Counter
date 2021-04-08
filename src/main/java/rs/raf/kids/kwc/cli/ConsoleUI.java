package rs.raf.kids.kwc.cli;

import rs.raf.kids.kwc.Stoppable;
import rs.raf.kids.kwc.cli.command.Commands;
import rs.raf.kids.kwc.util.Utils;

import java.util.Arrays;
import java.util.Scanner;

public class ConsoleUI implements Runnable, Stoppable {

    private volatile boolean running;

    private final Scanner scanner;

    private final Commands commands;

    public ConsoleUI() {
        scanner = new Scanner(System.in);
        commands = new Commands();
        commands.addCommand(Commands.addDirectoryCommand());
        commands.addCommand(Commands.addUrlCommand());
        commands.addCommand(Commands.stopCommand());
        commands.addCommand(Commands.printThreadsCommand());
        commands.addCommand(Commands.getResultCommand());
        commands.addCommand(Commands.queryResultCommand());
        commands.addCommand(Commands.printLinksCommand());
        commands.addCommand(Commands.clearFileSummaryCommand());
        commands.addCommand(Commands.clearWebSummaryCommand());
    }

    @Override
    public void run() {
        signalRun();
        while (isRunning()) {
            acceptUserInput();
            Utils.sleepThread(250);
        }
        scanner.close();
    }

    private void acceptUserInput() {
        String input = scanner.nextLine();
        processCommand(input);
    }

    private void processCommand(String command) {
        String[] cmdArgs = command.split(" ");
        String cmd = cmdArgs[0];
        String[] args = Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length);
        try {
            commands.execute(cmd, args);
        } catch (Exception ex) {
            Logger.error("Command execution error: " + ex.getMessage());
        }
    }

    @Override
    public void stop() {
        signalStop();
    }

    public boolean isRunning() {
        return running;
    }

    private void signalRun() {
        running = true;
        Logger.warn("ConsoleUI is now running");
        Utils.sleepThread(500);
    }

    private void signalStop() {
        Logger.warn("Stopping ConsoleUI...");
        running = false;
    }
}
