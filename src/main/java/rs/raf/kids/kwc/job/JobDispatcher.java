package rs.raf.kids.kwc.job;

import rs.raf.kids.kwc.Stoppable;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.pool.AbstractScannerPool;

import java.util.HashMap;
import java.util.Map;

public class JobDispatcher implements Runnable, Stoppable {

    private volatile boolean running;

    private final ScanningJobQueue jobQueue;

    private final Map<ScanType, AbstractScannerPool<? super ScanningJob>> scannerPools;

    public JobDispatcher(ScanningJobQueue jobQueue) {
        this.jobQueue = jobQueue;
        scannerPools = new HashMap<>();
    }

    @Override
    public void run() {
        signalRun();
        while (isRunning()) {
            initiateNextAvailableScanningJob();
        }
    }

    /**
     * Takes next job from <code>ScanningJobQueue</code> and delegates
     * its execution to corresponding <code>ScannerPool</code> based on
     * scan type. Taking a job of type <code>ScanType.TERMINATE</code>
     * causes <code>JobDispatcher</code> to stop its execution.
     */
    private void initiateNextAvailableScanningJob() {
        ScanningJob nextJob = jobQueue.next();
        ScanType scanType = nextJob.getScanType();
        switch (scanType) {
            case FILE, WEB -> scannerPools.get(scanType).accept(nextJob);
            case TERMINATE -> stop();
        }
    }

    /**
     * Binds given <code>AbstractScannerPool</code> to a scan type, which
     * causes any jobs of the given scan type to be passed directly to the
     * registered pool for execution.
     * @param scanType job scan type to be passed to given the pool
     * @param scannerPool a pool that executes all jobs of given type
     */
    public void registerScannerPool(ScanType scanType, AbstractScannerPool scannerPool) {
        scannerPools.put(scanType, scannerPool);
    }

    @Override
    public void stop() {
        signalStop();
    }

    public boolean isRunning() {
        return running;
    }

    private void signalRun() {
        running = true;
        Logger.warn("JobDispatcher is now running");
    }

    private void signalStop() {
        running = false;
        Logger.warn("Stopping JobDispatcher...");
    }
}
