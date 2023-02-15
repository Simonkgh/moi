package net.vonos.process;

import java.util.concurrent.TimeUnit;

/**
 * An interface equivalent to java.lang.Process. See ProcessFactory for more information.
 * <p>
 * There are no getInputStream() or getErrorStream() methods here because the ProcessDesc class takes
 * "consumer" objects as parameters instead; that is a safer way to handle processing of output from
 * an external process.
 * </p>
 */
public interface ProcessHandle {
    /**
     * Return true if the process is still running.
     */
    boolean isAlive();

    /**
     * Block until the process terminates or the timeout expires.
     *
     * @return true when the process has terminated (ie false when timeout expired)
     */
    boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Return the exit-status of the application. Only valid after waitFor(..) returns true.
     */
    int exitValue();

    /**
     * Terminate the process.
     * <p>
     * Note: on unix this sends a SIGTERM to the process. If the started application spawns child processes, then the
     * parent should trap SIGTERM and explicitly forward it to the child processes, eg as described here:
     *   http://veithen.github.io/2014/11/16/sigterm-propagation.html
     * </p>
     * <p>
     * If the started process is a shell which starts exactly one child process then instead of catching/relaying
     * signals it may use "exec ..." to replace itself with another process.
     * </p>
     */
    void destroyForcibly();
}
