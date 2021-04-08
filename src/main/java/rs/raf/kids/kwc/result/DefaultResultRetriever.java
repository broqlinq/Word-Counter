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

    /**
     * Holds all the <code>Future</code> result objects for submitted
     * scanning jobs.
     */
    private final Map<String, Future<Map<String, Integer>>> scanResults;

    /**
     * A cache for scan summaries.
     */
    private final Map<ScanType, Future<Map<String, Map<String, Integer>>>> scanSummaries;

    private final ExecutorService executorService;

    public DefaultResultRetriever() {
        scanResults = new ConcurrentHashMap<>();
        scanSummaries = new ConcurrentHashMap<>();
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Gets scan result for given query. Caller thread is blocked
     * until computation has finished.
     * @param query a query to search results by
     * @return result of keyword count for a query
     * @throws IllegalArgumentException if the passed query
     * is not found in result map
     */
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

    /**
     * Gets scan result for given query. Unlike <code>getResult</code>,
     * caller thread is not blocked if the computation is not finished,
     * but the exception is thrown in that case.
     * @param query a query to search results by
     * @return result of keyword count for a query
     * @throws IllegalArgumentException if the passed query
     * is not found in result map
     * @throws IllegalStateException if the computation is not yet finished
     */
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

    /**
     * Clears scan summary for a given scan type. Upon next <code>get</code>
     * or <code>query</code>, a new summary task will be initiated.
     * @param scanType
     */
    @Override
    public void clearSummary(ScanType scanType) {
        switch (scanType) {
            case FILE, WEB -> scanSummaries.remove(scanType);
            default        -> throw new IllegalArgumentException("'summary' query not supported for scan type: " + scanType);
        }
    }

    /**
     * Gets a summary of scan results for a given scan types. In case of
     * <code>ScanType.FILE</code>, result will be scan results for all
     * found corpus directories. In case of <code>ScanType.WEB</code>,
     * result will be keyword count for all domains that were found during
     * scan.
     * @param scanType scan type to get summary for
     * @return keyword count in corpus directories / domains
     */
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

    /**
     * Submits a new summary task for specified scan type.
     * @param scanType a scan type to get summary for
     * @param summaryTask a task that will be executed
     * @return a <code>Future</code> for passed task
     */
    private Future<Map<String, Map<String, Integer>>> submitSummaryTask(ScanType scanType, Callable<Map<String, Map<String, Integer>>> summaryTask) {
        Future<Map<String, Map<String, Integer>>> future = executorService.submit(summaryTask);
        scanSummaries.put(scanType, future);
        return future;
    }

    /**
     * Gets a summary for given type. Caller thread will be blocked if
     * until the computation has finished.
     * @param scanType a scan type to get summary for
     * @return a summary for passed scan type
     */
    private Map<String, Map<String, Integer>> getSummaryFor(ScanType scanType) {
        try {
            return scanSummaries.get(scanType).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("get summary failed: " + e.getMessage());
        }
    }

    /**
     * Calculates summary result for all <code>FileScanningJob</code> jobs.
     * @return a summary for all jobs of type <code>ScanType.FILE</code>
     */
    private Map<String, Map<String, Integer>> getFileScansSummary() {
        Set<String> fileResultKeys = getFileScanKeys();
        Map<String, Map<String, Integer>> result = new HashMap<>();
        for (String key : fileResultKeys) {
            result.put(key, getResult(key));
        }
        return result;
    }

    /**
     * Gets all queries for jobs of scan type <code>ScanType.FILE</code>.
     * @return queries for jobs of scan type <code>ScanType.FILE</code>
     */
    private Set<String> getFileScanKeys() {
        return scanResults.keySet()
                .stream()
                .filter(q -> q.startsWith("file|"))
                .collect(Collectors.toSet());
    }

    /**
     * Calculates summary result for all <code>WebScanningJob</code> jobs.
     * @return a summary for all jobs of type <code>ScanType.WEB</code>
     */
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

    /**
     * Gets all queries for <code>WebScanningJob</code> jobs.
     * @return queries for jobs of scan type <code>ScanType.WEB</code>
     */
    private Set<String> getWebScanKeys() {
        return scanResults.keySet()
                .stream()
                .filter(q -> q.startsWith("web|"))
                .collect(Collectors.toSet());
    }

    /**
     * Gets a summary for given type. Unlike <code>getSummary</code>,
     * a caller will not be blocked if the computation is not yet
     * finished, but the exception will be thrown.
     * @param scanType a scan type to get summary for
     * @return a summary for passed scan type
     * @throws IllegalStateException if the summary calcualtion
     * is not yet finished
     */
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

    /**
     * Adds a <code>Future</code> result object for a given query.
     * @param query a key for result
     * @param corpusResult result of computation for given query
     */
    @Override
    public void addCorpusResult(String query, Future<Map<String, Integer>> corpusResult) {
        scanResults.put(query, corpusResult);
    }

    @Override
    public void stop() {
        Logger.warn("Stopping ResultRetriever...");
        executorService.shutdown();
    }
}
