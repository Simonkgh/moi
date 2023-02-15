package net.vonos.process;

/**
 * Interface through which external processes can be started.
 * <p>
 * The standard Java ProcessBuilder class is unfortunately extremely unit-test-unfriendly; this is better.
 * </p>
 */
public interface ProcessFactory {
    ProcessDesc newProcessDesc();
}
