package rs.raf.kids.kwc.result;

import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.job.ScanType;
import rs.raf.kids.kwc.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DefaultResultRetriever implements ResultRetriever {
    private final Map<String, Future<Map<String, Integer>>> scanResults;
    private final Map<ScanType, Future<Map<String, Map<String, Integer>>>> scanSummaries;
    private final ExecutorService executorService;

    public DefaultResultRetriever() {
        scanResults = new ConcurrentHashMap<>();
        scanSummaries = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public Map<String, Integer> getResult(String query) {
        if (!scanResults.containsKey(query))
            throw new IllegalArgumentException("No corpus result found for query: " + query);

        try {
            return scanResults.get(query).get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(query + ": " + e.getMessage());
        }
        return Map.of();
    }

    @Override
    public Map<String, Integer> queryResult(String query) {
        if (!scanResults.containsKey(query))
            throw new IllegalArgumentException("No corpus result found for query: " + query);

        Future<Map<String, Integer>> result = scanResults.get(query);
        if (!result.isDone())
            throw new IllegalStateException("Result is still being calculated for query: " + query);

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(query + ": " + e.getMessage());
        }
        return Map.of();
    }

    @Override
    public void clearSummary(ScanType scanType) {
        switch (scanType) {
            case FILE, WEB -> scanSummaries.remove(scanType);
            default        -> throw new IllegalArgumentException("'summary' query not supported for scan type: " + scanType);
        }
    }

    @Override
    public Map<String, Map<String, Integer>> getSummary(ScanType scanType) {
        if (scanSummaries.containsKey(scanType)) {
            Logger.debugWarn("summary already calculated for: " + scanType);
            return getSummaryFor(scanType);
        }

        Logger.debugWarn("Calculating summary for: " + scanType);
        switch (scanType) {
            case FILE -> submitSummaryTask(ScanType.FILE, this::getFileScansSummary);
            case WEB  -> submitSummaryTask(ScanType.WEB, this::getWebScansSummary);
            default   -> throw new IllegalArgumentException("'summary' not supported for scan type: " + scanType);
        }
        return getSummaryFor(scanType);
    }

    private Future<Map<String, Map<String, Integer>>> submitSummaryTask(ScanType scanType, Callable<Map<String, Map<String, Integer>>> summaryTask) {
        Future<Map<String, Map<String, Integer>>> future = executorService.submit(summaryTask);
        scanSummaries.put(scanType, future);
        return future;
    }

    private Map<String, Map<String, Integer>> getSummaryFor(ScanType scanType) {
        try {
            return scanSummaries.get(scanType).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("get summary failed: " + e.getMessage());
        }
    }

    private Map<String, Map<String, Integer>> getFileScansSummary() {
        Set<String> fileResultKeys = getFileScanKeys();
        Map<String, Map<String, Integer>> result = new HashMap<>();
        for (String key : fileResultKeys) {
            result.put(key, getResult(key));
        }
        return result;
    }

    private Set<String> getFileScanKeys() {
        return scanResults.keySet()
                .stream()
                .filter(q -> q.startsWith("file|"))
                .collect(Collectors.toSet());
    }

    private Map<String, Map<String, Integer>> getWebScansSummary() {
        Set<String> webResultKeys = getWebScanKeys();
        Set<String> urls = webResultKeys.stream()
                .map(k -> k.substring(4))
                .collect(Collectors.toSet());
        Set<String> domains = Utils.extractDomains(urls);
        Map<String, Map<String, Integer>> result = Utils.initMap(domains, Utils::initKeywordsMap);
        for (String key : webResultKeys) {
            Map<String, Integer> webResult = getResult(key);
            String domain = Utils.extractDomain(key.substring(4));
            Map<String, Integer> domainResult = result.get(domain);
            webResult.forEach((wk, wv) -> domainResult.merge(wk, wv, Integer::sum));
        }
        Utils.sleepThread(12000);
        return result;
    }

    private Set<String> getWebScanKeys() {
        return scanResults.keySet()
                .stream()
                .filter(q -> q.startsWith("web|"))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Map<String, Integer>> querySummary(ScanType scanType) {
        if (!scanSummaries.containsKey(scanType)) {
            switch (scanType) {
                case FILE -> submitSummaryTask(scanType, this::getFileScansSummary);
                case WEB  -> submitSummaryTask(scanType, this::getWebScansSummary);
                default   -> throw new IllegalArgumentException("'summary' not supported for scan type: " + scanType);
            }
        }

        Future<Map<String, Map<String, Integer>>> future = scanSummaries.get(scanType);
        if (!future.isDone())
            throw new IllegalStateException("Summary is still being calculated for: " + scanType);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(scanType + " summary: " + e.getMessage());
        }
        return Map.of();
    }

    @Override
    public void addCorpusResult(String corpus, Future<Map<String, Integer>> corpusResult) {
        scanResults.put(corpus, corpusResult);
    }

    @Override
    public void stop() {
        Logger.warn("Stopping ResultRetriever...");
        executorService.shutdown();
    }
}
