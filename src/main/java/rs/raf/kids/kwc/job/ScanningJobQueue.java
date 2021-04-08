package rs.raf.kids.kwc.job;

import rs.raf.kids.kwc.util.Utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ScanningJobQueue {

    private final BlockingQueue<ScanningJob> scanningJobs;

    public ScanningJobQueue() {
        scanningJobs = new LinkedBlockingQueue<>();
    }

    /**
     * Safely puts a <code>ScanningJob</code> into queue.
     * @param job a job to be put into queue.
     */
    public void submit(ScanningJob job) {
        try {
            scanningJobs.put(job);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Returns a next in line <code>ScanningJob</code> from the queue.
     * If no jobs are present, a caller <code>Thread</code> will wait
     * until queue is not empty.
     * @return Next <code>ScanningJob</code> from queue.
     * @throws IllegalStateException In case that caller thread
     * is not <code>JobDispatcherThread</code>.
     */
    public ScanningJob next() {
        if (Utils.isJobDispatcherThread()) {
            try {
                return scanningJobs.take();
            } catch (InterruptedException ignored) {}
            return ScanningJob.TERMINAL_JOB;
        }
        else throw new IllegalStateException("Only JobDispatcherThread can read from ScanningJobQueue");
    }

    public void terminate() throws InterruptedException {
        scanningJobs.put(ScanningJob.TERMINAL_JOB);
    }
}
