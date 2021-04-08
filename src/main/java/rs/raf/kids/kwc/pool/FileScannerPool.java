package rs.raf.kids.kwc.pool;

import rs.raf.kids.kwc.job.FileScanningJob;
import rs.raf.kids.kwc.job.ScanningJobQueue;
import rs.raf.kids.kwc.result.ResultRetriever;

public class FileScannerPool extends AbstractScannerPool<FileScanningJob> {

    public FileScannerPool(ScanningJobQueue jobQueue, ResultRetriever resultRetriever) {
        super(jobQueue, resultRetriever);
    }

}
