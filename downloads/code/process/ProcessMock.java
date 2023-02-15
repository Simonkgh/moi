package net.vonos.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementations of the ProcessFactory interface and related types which are convenient for unit-testing.
 * <p>
 * These implementations can be preconfigured to return specific values (like mock-objects) and save parameters
 * passed to them for later verification by unit-test assertions.
 * </p>
 */
public class ProcessMock {
    // Simple factory methods

    public static Factory newFactory(Desc... descs) {
        Factory pf = new Factory();
        pf.pds.addAll(Arrays.asList(descs));
        return pf;
    }

    public static Desc newDesc(Handle... handles) {
        Desc pd = new Desc();
        pd.phs.addAll(Arrays.<Handle>asList(handles));
        return pd;
    }

    public static Handle newHandle() {
        return new Handle();
    }

    public static class Factory implements ProcessFactory {
        // List of preconfigured objects to be returned from newProcessDesc.
        private List<Desc> pds = new ArrayList<>();
        private int pdIndex;

        public Factory add(Desc pd) {
            pds.add(pd);
            return this;
        }

        @Override
        public ProcessDesc newProcessDesc() {
            if (pdIndex < pds.size()) {
                return pds.get(pdIndex++);
            }
            throw new IllegalStateException("No more pds available");
        }
    }

    public static class Desc implements ProcessDesc {
        // Mocked outputs
        private List<Handle> phs = new ArrayList<>();
        private int phIndex;

        // Captured Inputs
        private List<String> command;
        private File dir;
        private Consumer<String> inputConsumer;
        private Consumer<String> errorConsumer;

        // Configuration methods

        public void add(Handle ph) {
            phs.add(ph);
        }

        public Handle mockHandle() {
            Handle ph = new Handle();
            phs.add(ph);
            return ph;
        }

        // Query methods

        public List<String> command() {
            return command;
        }

        public File directory() {
            return dir;
        }

        // ProcessDesc implementations

        @Override
        public ProcessDesc command(List<String> args) {
            this.command = new ArrayList<>(args);
            return this;
        }

        @Override
        public ProcessDesc directory(File dir) {
            this.dir = dir;
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
            if (phIndex < phs.size()) {
                Handle ph = phs.get(phIndex++);
                ph.init(inputConsumer, errorConsumer);
                return ph;
            }
            throw new IllegalStateException("No more phs available");
        }
    }

    public static class Handle implements ProcessHandle {
        // Mocked Outputs
        private int exitValue;
        private long durationMillis;
        private List<String> inputs;
        private List<String> errors;

        private long terminatesAt;

        // Captured Inputs
        private boolean isStarted;
        private boolean isDestroyed;

        // Configuration methods
        public Handle exitValue(int val) {
            exitValue = val;
            return this;
        }

        public Handle duration(long timeout, TimeUnit unit) {
            durationMillis = unit.toMillis(timeout);
            return this;
        }

        public Handle inputs(List<String> val) {
            inputs = val;
            return this;
        }

        public Handle errors(List<String> val) {
            errors = val;
            return this;
        }

        void init(Consumer<String> inputConsumer, Consumer<String> errorConsumer) {
            isStarted = true;
            terminatesAt = System.currentTimeMillis() + durationMillis;

            if (inputs != null) {
                // Assume inputConsumer is not null; if a test defines inputs it is assuming there is something
                // to consume it, and it would be a valid test failure if there is no such consumer.
                inputs.forEach(inputConsumer::accept);
            }

            if (errors != null) {
                // Assume errorConsumer is not null; if a test defines inputs it is assuming there is something
                // to consume it, and it would be a valid test failure if there is no such consumer.
                errors.forEach(errorConsumer::accept);
            }
        }

        // Query methods

        public boolean isStarted() {
            return isStarted;
        }

        public boolean isDestroyed() {
            return isDestroyed;
        }

        // ProcessHandle implementations

        @Override
        public boolean isAlive() {
            return System.currentTimeMillis() < terminatesAt;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            long untilTermination = terminatesAt - System.currentTimeMillis();
            long millisToSleep = Math.min(unit.toMillis(timeout), untilTermination);
            if (millisToSleep > 0) {
                Thread.sleep(millisToSleep);
            }
            return !isAlive(); // finished --> true
        }

        @Override
        public int exitValue() {
            return exitValue;
        }

        @Override
        public void destroyForcibly() {
            isDestroyed = true;
        }
    }
}
