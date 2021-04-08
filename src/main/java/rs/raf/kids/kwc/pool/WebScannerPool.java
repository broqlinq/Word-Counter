package rs.raf.kids.kwc.pool;

import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.job.ScanningJobQueue;
import rs.raf.kids.kwc.job.WebScanningJob;
import rs.raf.kids.kwc.result.ResultRetriever;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class WebScannerPool extends AbstractScannerPool<WebScanningJob> {

    private static final Map<URI, UrlInfo> visitedUrlInfoMap = new ConcurrentHashMap<>();

    public WebScannerPool(ScanningJobQueue jobQueue, ResultRetriever resultRetriever) {
        super(jobQueue, resultRetriever);
    }

    public static boolean isUrlReadyToScan(String url) {
        try {
            URI uri = new URL(url).toURI();
            return checkIfReadyToScan(uri);
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    public static boolean checkIfReadyToScan(URI uri) {
        long now = System.currentTimeMillis();
        if (visitedUrlInfoMap.containsKey(uri)) {
            long exp = visitedUrlInfoMap.get(uri).expirationTime;
            if (now >= exp) {
                visitedUrlInfoMap.get(uri).expirationTime = now + AppConfig.urlRefreshTime;
                return true;
            }
            return false;
        }
        UrlInfo info = new UrlInfo(uri, now);
        visitedUrlInfoMap.put(uri, info);
        return true;
    }

    private static class UrlInfo implements Comparable<UrlInfo> {
        private final URI uri;
        private long expirationTime;

        public UrlInfo(URI uri, long expirationTime) {
            this.uri = uri;
            this.expirationTime = expirationTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UrlInfo urlInfo = (UrlInfo) o;
            return uri.equals(urlInfo.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }

        @Override
        public int compareTo(UrlInfo that) {
            return this.uri.compareTo(that.uri);
        }
    }
}
