package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;

public class StopCommand implements Command {

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 0)
            throw new IllegalArgumentException('\'' + getName() + "' command does not take any arguments");

        Main.console.stop();
    }
}
