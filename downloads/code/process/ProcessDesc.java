// Author: Simon Kitching
// This code is in the public domain
package net.vonos.process;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * An interface equivalent to java.lang.ProcessBuilder. See ProcessFactory for more information.
 */
public interface ProcessDesc {
    /** Sets the application to start, and its commandline-arguments. */
    ProcessDesc command(List<String> args);

    /** Sets the current-working-directory for the new process. */
    ProcessDesc directory(File dir);

    /** Sets a callback which is invoked for each LF-terminated line that the process writes to its STDOUT. */
    ProcessDesc inputConsumer(Consumer<String> consumer);

    /** Sets a callback which is invoked for each LF-terminated line that the process writes to its STDERR. */
    ProcessDesc errorConsumer(Consumer<String> consumer);

    /** Start the external process (which also starts callbacks to the inputConsumer and errorConsumer). */
    ProcessHandle start() throws IOException;
}
