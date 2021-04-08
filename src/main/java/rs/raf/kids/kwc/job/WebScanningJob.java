package rs.raf.kids.kwc.job;

import rs.raf.kids.kwc.config.AppConfig;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class WebScanningJob implements ScanningJob {

    private final String url;

    private final int urlDepth;

    public WebScanningJob(String url) {
        this(url, AppConfig.urlDepthLimit);
    }

    public WebScanningJob(String url, int urlDepth) {
        this.url = url;
        this.urlDepth = urlDepth;
    }

    @Override
    public ScanType getScanType() {
        return ScanType.WEB;
    }

    @Override
    public String getQuery() {
        return "web|" + url;
    }

    @Override
    public Future<Map<String, Integer>> initiate(ExecutorService executorService) {
        return executorService.submit(new WebScanningTask(url, urlDepth));
    }
}
