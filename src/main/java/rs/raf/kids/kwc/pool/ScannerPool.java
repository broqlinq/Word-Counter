package rs.raf.kids.kwc.pool;

import rs.raf.kids.kwc.job.ScanningJob;

public interface ScannerPool<T extends ScanningJob> {
    void accept(T job);
}
