package rs.raf.kids.kwc.pool;

import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.job.ScanningJob;
import rs.raf.kids.kwc.job.ScanningJobQueue;
import rs.raf.kids.kwc.result.ResultRetriever;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AbstractScannerPool<T extends ScanningJob> implements ScannerPool<T> {

    protected final ScanningJobQueue jobQueue;

    protected final ExecutorService executorService;

    protected final ResultRetriever resultRetriever;

    public AbstractScannerPool(ScanningJobQueue jobQueue, ResultRetriever resultRetriever) {
        this.jobQueue = jobQueue;
        this.resultRetriever = resultRetriever;
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void accept(T job) {
        Future<Map<String, Integer>> futureResult = job.initiate(executorService);
        Logger.debugWarn("Adding query to retriever: " + job.getQuery());
        resultRetriever.addCorpusResult(job.getQuery(), futureResult);
    }

    public void terminate() {
        executorService.shutdown();
    }
}
