package rs.raf.kids.kwc.job;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public interface ScanningJob {

    ScanType getScanType();

    String getQuery();

    Future<Map<String, Integer>> initiate(ExecutorService executorService);

    /**
     * Represents a job which should be used as signal for
     * ScanningJobQueue thread to stop. Calling any methods
     * except <code>getScanType</code> will throw
     * <code>UnsupportedOperationException<code/>.
     */
    ScanningJob TERMINAL_JOB = new ScanningJob() {
        @Override
        public ScanType getScanType() {
            return ScanType.TERMINATE;
        }

        @Override
        public String getQuery() {
            throw new UnsupportedOperationException("Terminal job doesn't support getQuery operation");
        }

        @Override
        public Future<Map<String, Integer>> initiate(ExecutorService executorService) {
            throw new UnsupportedOperationException("Terminal job doesn't support initiate operation");
        }
    };
}
