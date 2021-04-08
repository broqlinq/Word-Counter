package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.job.ScanType;
import rs.raf.kids.kwc.util.Utils;

import java.util.Map;

public class QueryResultCommand implements Command {

    @Override
    public String getName() {
        return "query";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 1)
            throw new IllegalArgumentException('\'' + getName() + "' takes 1 argument, but " + args.length + " were passed");

        String query = args[0];
        String[] queryArgs = query.split("\\|");
        if (queryArgs.length != 2)
            throw new IllegalArgumentException('\'' + getName() + "' argument has invalid form");

        parseQuery(query);
    }

    private void parseQuery(String query) {
        String[] args = query.split("\\|");
        ScanType scanType = switch (args[0]) {
            case "file" -> ScanType.FILE;
            case "web" -> ScanType.WEB;
            default -> throw new IllegalArgumentException("Invalid scan type '" + args[0] + '\'');
        };
        try {
            if (args[1].equals("summary")) {
                var result = Main.resultRetriever.querySummary(scanType);
                Utils.prettyPrintSummaryResult(scanType, result);
            } else {
                var result = Main.resultRetriever.queryResult(query);
                Utils.prettyPrintResult(query, result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
