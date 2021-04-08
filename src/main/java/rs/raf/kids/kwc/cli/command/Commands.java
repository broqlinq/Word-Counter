package rs.raf.kids.kwc.cli.command;

import java.util.HashMap;

public class Commands {

    private static Command addDirectoryCommand = new AddDirectoryCommand();
    private static Command addUrlCommand = new AddUrlCommand();
    private static Command stopCommand = new StopCommand();
    private static Command getResultCommand = new GetResultCommand();
    private static Command queryResultCommand = new QueryResultCommand();
    private static Command printThreadsCommand = new PrintThreadsCommand();
    private static Command printLinksCommand = new PrintLinksCommand();
    private static Command clearFileSummaryCommand = new ClearFileSummaryCommand();
    private static Command clearWebSummaryCommand = new ClearWebSummaryCommand();

    public static Command addDirectoryCommand() {
        return addDirectoryCommand;
    }
    public static Command stopCommand() {
        return stopCommand;
    }
    public static Command getResultCommand() {
        return getResultCommand;
    }
    public static Command queryResultCommand() {
        return queryResultCommand;
    }
    public static Command printThreadsCommand() {
        return printThreadsCommand;
    }
    public static Command printLinksCommand() {
        return printLinksCommand;
    }
    public static Command clearFileSummaryCommand() {
        return clearFileSummaryCommand;
    }
    public static Command clearWebSummaryCommand() {
        return clearWebSummaryCommand;
    }

    private final HashMap<String, Command> commands = new HashMap<>();

    public static Command addUrlCommand() {
        return addUrlCommand;
    }

    public void addCommand(Command command) {
        commands.put(command.getName(), command);
    }

    public void execute(String cmd, String... args) {
        if (!commands.containsKey(cmd))
            throw new IllegalArgumentException("Command '" + cmd + "' was not recognized");

        commands.get(cmd).execute(args);
    }
}
