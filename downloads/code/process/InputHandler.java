package net.vonos.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.function.Consumer;

/**
 * Continuously reads data from an input-stream, splits it into lines, and passes it to a Consumer object.
 * <p>
 * External processes need to have their input and error streams regularly read, as they may be fixed-size
 * buffers which will cause the external process to block when they are full. An instance of this type,
 * run as a thread, will ensure that is done - and each line is passed to the specified consumer (if any).
 * When no consumer is set, the data read is just discarded.
 * </p>
 * <p>
 * This thread terminates automatically when EOF is encountered - which is guaranteed when the external
 * process being read from has been terminated.
 * </p>
 */
public class InputHandler extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(InputHandler.class);

    // Stream to read from
    private final InputStream is;

    // Consumer to pass each line to (may be null)
    private final Consumer<String> consumer;

    private final BufferedReader reader;

    /** Constructor. */
    public InputHandler(InputStream is, Consumer<String> consumer) {
        super("Process input"); // name thread for debugging purposes
        this.is = is;
        this.consumer = consumer;
        this.reader = new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Thread run method, which keeps processing output from the inputstream until EOF or exception.
     */
    @Override
    public void run() {
        try {
            for(;;) {
                String line = reader.readLine();
                if (line == null) {
                    break; // EOF
                }

                if (consumer != null) {
                    consumer.accept(line);
                }
            }
        } catch (IOException e) {
            LOG.info("IOException", e);
        } finally {
            try {
                reader.close();
            } catch(IOException e) {
                LOG.warn("Exception while closing reader", e);
            }
        }
    }
}
