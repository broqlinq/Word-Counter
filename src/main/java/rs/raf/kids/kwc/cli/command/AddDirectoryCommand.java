package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;
import rs.raf.kids.kwc.cli.Logger;

public class AddDirectoryCommand implements Command {

    @Override
    public String getName() {
        return "ad";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 1)
            throw new IllegalArgumentException('\'' + getName() + "' takes 1 argument, but " + args.length + " were passed");

        String dirPath = args[0];
        Main.directoryCrawler.addDirectory(dirPath);
    }
}
