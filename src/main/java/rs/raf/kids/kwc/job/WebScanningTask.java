package rs.raf.kids.kwc.job;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import rs.raf.kids.kwc.Main;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.pool.WebScannerPool;
import rs.raf.kids.kwc.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class WebScanningTask implements Callable<Map<String, Integer>> {

    private final String urlToScan;

    private final Set<String> keywords;

    private final int urlDepth;

    public WebScanningTask(String urlToScan, int urlDepth) {
        this.urlToScan = urlToScan;
        this.urlDepth = urlDepth;
        keywords = AppConfig.keywords;
    }

    @Override
    public Map<String, Integer> call() {
        Logger.debugInfo("URL depth = " + urlDepth);
        Logger.info("Started web scan for: web|" + urlToScan);
        try {
            Document doc = Jsoup.connect(urlToScan).get();
            if (urlDepth > 0) {
                checkForInnerUrls(doc);
            }
            return scanDocument(doc);
        } catch (IOException e) {
            Logger.error(urlToScan + "Web Scanning Error:\n" + e.getMessage());
        }
        return Map.of();
    }

    private Map<String, Integer> scanDocument(Document doc) {
        Map<String, Integer> result = Utils.initKeywordsMap();
        List<String> words = keywordsInDocument(doc);
        for (String word : words) {
            result.compute(word, (k,v) -> v + 1);
        }
        Logger.debugInfo("Finished web scan for: web|" + urlToScan);
        return result;
    }

    private List<String> keywordsInDocument(Document doc) {
//        Logger.debugWarn(urlToScan + " :: " + Arrays.toString(doc.text().split(" ")));
        return Arrays.stream(doc.text().split(" "))
                .map(Utils::removePunctuation)
                .map(String::trim)
                .filter(keywords::contains)
                .toList();
    }

    private void checkForInnerUrls(Document doc) {
        doc.select("a[href]")
                .stream()
                .map(link -> link.attr("abs:href"))
                .filter(this::isLinkValid)
                .filter(this::isLinkReadyToScan)
                .forEach(this::submitUrlScanningJob);
    }

    private boolean isLinkValid(String link) {
        try {
            URL url = new URL(link);
            url.toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    private boolean isLinkReadyToScan(String link) {
        try {
            URI uri = new URL(link).toURI();
            return WebScannerPool.checkIfReadyToScan(uri);
        } catch (Exception e) {
            Logger.debugError(e.getMessage());
            return false;
        }
    }

    private void submitUrlScanningJob(String url) {
        ScanningJob scanningJob = new WebScanningJob(url, urlDepth - 1);
        Main.scanningJobQueue.submit(scanningJob);
    }
}
