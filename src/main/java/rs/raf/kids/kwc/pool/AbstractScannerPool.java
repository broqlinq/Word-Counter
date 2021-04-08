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

    /**
     * An executor for given scanning jobs.
     */
    protected final ExecutorService executorService;

    /**
     * Upon submitting a job, its corresponding <code>Future</code>
     * object is put in <code>ResultRetriever</code>.
     */
    protected final ResultRetriever resultRetriever;

    public AbstractScannerPool(ScanningJobQueue jobQueue, ResultRetriever resultRetriever) {
        this.resultRetriever = resultRetriever;
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Accepts a scanning job and submits it for execution. The result of
     * submission is put into <code>ResultRetriever</code>.
     * @param job task to submit for execution
     */
    @Override
    public void accept(T job) {
        Future<Map<String, Integer>> futureResult = job.initiate(executorService);
        Logger.debugWarn("Adding query to retriever: " + job.getQuery());
        resultRetriever.addCorpusResult(job.getQuery(), futureResult);
    }

    /**
     * Shuts down executor service.
     */
    public void terminate() {
        executorService.shutdown();
    }
}
