package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;
import rs.raf.kids.kwc.job.ScanType;

public class ClearWebSummaryCommand implements Command {
    @Override
    public String getName() {
        return "csw";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 0)
            throw new IllegalArgumentException('\'' + getName() + "' command does not take any arguments");

        Main.resultRetriever.clearSummary(ScanType.WEB);
    }
}
