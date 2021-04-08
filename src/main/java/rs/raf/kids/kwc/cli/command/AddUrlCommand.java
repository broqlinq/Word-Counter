package rs.raf.kids.kwc.cli.command;

import rs.raf.kids.kwc.Main;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.job.WebScanningJob;
import rs.raf.kids.kwc.pool.WebScannerPool;
import rs.raf.kids.kwc.util.Utils;

public class AddUrlCommand implements Command {

    @Override
    public String getName() {
        return "aw";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 1)
            throw new IllegalArgumentException('\'' + getName() + "' takes 1 argument, but " + args.length + " were passed");

        String url = args[0];
        if (!Utils.isValidUrl(url)) {
            throw new IllegalArgumentException("Invalid url: " + url);
        } else if (WebScannerPool.isUrlReadyToScan(url)){
            Main.scanningJobQueue.submit(new WebScanningJob(url));
        } else {
            Logger.debugError("Already visited url: " + url);
        }
    }
}
