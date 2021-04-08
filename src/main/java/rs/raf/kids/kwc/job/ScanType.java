package rs.raf.kids.kwc.job;

/**
 * <p>
 *   An enum that holds all scan types which are supported,
 *   currently <code>ScanType.FILE</code> and <code>ScanType.WEB</code>.
 * </p>
 * <p>
 *   Note that <code>ScanType.TERMINATE</code> represents
 *   value to be used as signal for job queue to terminate
 *   its execution.
 * </p>
 */
public enum ScanType {
    TERMINATE,
    FILE,
    WEB
}
