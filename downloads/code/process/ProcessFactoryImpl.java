package net.vonos.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A simple factory for ProcessBuilder instances and its related types.
 * <p>
 * The inner-classes defined here are 1:1 wrappers around java.lang.ProcessBuilder and java.lang.Process. However
 * unlike the standard classes, these implement interfaces so they can be mocked for testing; the native classes
 * are both concrete and final, so any code that uses them directly cannot be unit-tested with Mockito or similar.
 * </p>
 */
@Component
public class ProcessFactoryImpl implements ProcessFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessFactoryImpl.class);

    public ProcessDesc newProcessDesc() {
        return new ProcessDescImpl();
    }

    /** Wraps a java.lang.ProcessBuilder instance. */
    private static class ProcessDescImpl implements ProcessDesc {
        private final ProcessBuilder builder = new ProcessBuilder();
        private Consumer<String> inputConsumer;
        private Consumer<String> errorConsumer;

        @Override
        public ProcessDesc command(List<String> args) {
            builder.command(args);
            return this;
        }

        @Override
        public ProcessDesc directory(File dir) {
            builder.directory(dir);
            return this;
        }

        @Override
        public ProcessDesc inputConsumer(Consumer<String> consumer) {
            this.inputConsumer = consumer;
            return this;
        }

        @Override
        public ProcessDesc errorConsumer(Consumer<String> consumer) {
            this.errorConsumer = consumer;
            return this;
        }

        @Override
        public ProcessHandle start() throws IOException {
            return new ProcessHandleImpl(builder, inputConsumer, errorConsumer);
        }
    }

    /** Wraps a java.lang.ProcessBuilder instance. */
    private static class ProcessHandleImpl implements ProcessHandle {
        private final Process process;
        private final InputHandler inputConsumer;
        private final InputHandler errorConsumer;

        ProcessHandleImpl(ProcessBuilder builder, Consumer<String> inputConsumer, Consumer<String> errorConsumer)
                throws IOException {
            process = builder.start();
            this.inputConsumer = new InputHandler(process.getInputStream(), inputConsumer);
            this.errorConsumer = new InputHandler(process.getErrorStream(), errorConsumer);

            this.inputConsumer.start();
            this.errorConsumer.start();
        }

        @Override
        public boolean isAlive() {
            return process.isAlive();
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            boolean finished = process.waitFor(timeout, unit);
            if (finished) {
                inputConsumer.join();
                errorConsumer.join();

                try {
                    process.getInputStream().close();
                    process.getOutputStream().close();
                    process.getErrorStream().close();
                } catch(IOException e) {
                    LOG.warn("Exception while closing process streams", e);
                }
            }
            return finished;
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public void destroyForcibly() {
            // Note: on unix this sends a SIGTERM to the process.
            // If the started application spawns child processes, then the parent should trap SIGTERM and
            // explicitly forward it to the child processes, eg as described here:
            //   http://veithen.github.io/2014/11/16/sigterm-propagation.html
            process.destroyForcibly();
        }
    }
}
