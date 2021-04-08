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

    /**
     * A URL to scan for keywords.
     */
    private final String urlToScan;

    /**
     * A list of keywords that are being counted.
     */
    private final Set<String> keywords;

    /**
     * Current depth of scan. Any value greater than <code>0</code> will cause
     * the job to initiate new <code>WebScanningJob</code> for any inner URL
     * that is detected.
     */
    private final int urlDepth;

    public WebScanningTask(String urlToScan, int urlDepth) {
        this.urlToScan = urlToScan;
        this.urlDepth = urlDepth;
        keywords = AppConfig.keywords;
    }

    /**
     * Fetches a HTML document from the URL and counts the occurrences
     * of keywords in that document. If current depth is greater than
     * <code>0</code>, new <code>WebScanningJob</code> will be created
     * and submitted to job queue for every URL found in the document.
     * @return occurrences of every keyword in HTML document
     */
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

    /**
     * Counts all occurrences of keywords in a document.
     * @param doc document that is scanned for keywords
     * @return occurrences of every keyword in given document
     */
    private Map<String, Integer> scanDocument(Document doc) {
        Map<String, Integer> result = Utils.initKeywordsMap();
        List<String> words = keywordsInDocument(doc);
        for (String word : words) {
            result.compute(word, (k,v) -> v + 1);
        }
        Logger.debugInfo("Finished web scan for: web|" + urlToScan);
        return result;
    }

    /**
     * Splits a document body into words, and cleans and filters them
     * based on the specified keywords.
     * @param doc document that is being split into words
     * @return a list of filtered words in document
     */
    private List<String> keywordsInDocument(Document doc) {
//        Logger.debugWarn(urlToScan + " :: " + Arrays.toString(doc.text().split(" ")));
        return Arrays.stream(doc.body().text().split(" "))
                .map(Utils::removePunctuation)
                .map(String::trim)
                .filter(keywords::contains)
                .toList();
    }

    /**
     * Scans document for any URL and creates a <code>WebScanningJob</code>
     * for every URL.
     * @param doc document that is being scanned for URLs
     */
    private void checkForInnerUrls(Document doc) {
        doc.select("a[href]")
                .stream()
                .map(link -> link.attr("abs:href"))
                .filter(Utils::isValidUrl)
                .filter(this::isLinkReadyToScan)
                .forEach(this::submitUrlScanningJob);
    }

    /**
     * Checks if the link, given as <code>String</code>, is ready to be scanned.
     * A link is ready to be scanned if it passes necessary checks in
     * <code>WebScannerPool</code>.
     * @param link
     * @return
     */
    private boolean isLinkReadyToScan(String link) {
        try {
            URI uri = new URL(link).toURI();
            return WebScannerPool.checkIfReadyToScan(uri);
        } catch (Exception e) {
            Logger.debugError(e.getMessage());
            return false;
        }
    }

    /**
     * Creates a new <code>WebScanningJob</code> for a given URL
     * and submits it to job queue.
     * @param url an URL to create a job for
     */
    private void submitUrlScanningJob(String url) {
        ScanningJob scanningJob = new WebScanningJob(url, urlDepth - 1);
        Main.scanningJobQueue.submit(scanningJob);
    }
}
