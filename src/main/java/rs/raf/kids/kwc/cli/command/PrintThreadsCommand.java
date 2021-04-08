package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;

public class PrintThreadsCommand implements Command {

    @Override
    public String getName() {
        return "tds";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 0)
            throw new IllegalArgumentException('\'' + getName() + "' command does not take any arguments");

        Main.printThreads();
    }
}
