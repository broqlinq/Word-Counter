package rs.raf.kids.kwc.result;

import rs.raf.kids.kwc.Stoppable;
import rs.raf.kids.kwc.job.ScanType;

import java.util.Map;
import java.util.concurrent.Future;

public interface ResultRetriever extends Stoppable {

    Map<String, Integer> getResult(String query);

    Map<String, Integer> queryResult(String query);

    void clearSummary(ScanType scanType);

    Map<String, Map<String, Integer>> getSummary(ScanType scanType);

    Map<String, Map<String, Integer>> querySummary(ScanType scanType);

    void addCorpusResult(String corpus, Future<Map<String, Integer>> corpusResult);
}
