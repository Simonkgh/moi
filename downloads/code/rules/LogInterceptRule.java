// Author Simon Kitching 2017.
// This software is in the public domain
package net.vonos.junit.rules;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Junit rule to capture all logmessages to a specific logging category.
 * <p>
 * This assumes that the underlying logging implementation is logback-classic.
 * </p>
 * <p>
 * Use as follows:
 * <pre>
 * @Rule
 * public final LogInterceptRule logRule = new LogInterceptRule();
 *
 * @Test
 * @LogInterceptRule.ForName("foo.bar")
 * public void testSomething() {
 *     ...
 *     Assert.assertEquals(2, logRule.getMessages().size());
 * }
 * </pre>
 * </p>
 */
public class LogInterceptRule implements TestRule {
    private final MemAppender memAppender;

    /** Constructor. */
    public LogInterceptRule() {
        this.memAppender = new MemAppender();
    }

    /**
     * Return the captured messages without formatting (no level, category, stacktraces or variable-interpolation).
     */
    public List<String> getRawMessages() {
        return memAppender.rawMessages;
    }

    /**
     * Return the captured messages with basic formatting, stacktraces and variable-interpolation.
     */
    public List<String> getMessages() {
        return memAppender.messages;
    }

    /**
     * Implement the junit-rule-behaviour: return a closure whose evaluate() method will run the specified "base"
     * statement, with logging-logic wrapped around it.
     */
    @Override
    public Statement apply(Statement base, Description description) {
        memAppender.clear();

        String catName = getCategory(description);
        if (catName == null) {
            return base;
        }

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(catName);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                logger.addAppender(memAppender);
                try {
                    base.evaluate();
                } finally {
                    logger.detachAppender(memAppender);
                }
            }
        };
    }

    // ====================================================================================================

    /**
     * Annotation type that can be used to specify logging-category to be captured by string.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ForName {
        String value();
    }

    /**
     * Annotation type that can be used to specify logging-category to be captured by class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ForClass {
        Class<?> value();
    }

    /**
     * Determine the logging-category to capture (if any) for a specific junit-test-method.
     */
    public static String getCategory(Description desc) {
        ForName fn = desc.getAnnotation(ForName.class);
        ForClass fc = desc.getAnnotation(ForClass.class);

        if ((fn == null) && (fc == null)) {
            return null;
        }

        if ((fn != null) && (fc == null)) {
            return fn.value();
        }

        if ((fc != null) && (fn == null)) {
            return fc.value().getName();
        }

        throw new IllegalArgumentException("Only one of ForName and ForClass may be specified");
    }

    /**
     * Custom SLF4J appender which saves messages into an in-memory list.
     * <p>
     * Only level WARN or above are kept. The logging format is hard-wired here so tests are not dependent
     * on the current logging configuration settings.
     * </p>
     */
    private static class MemAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        protected final ReentrantLock lock = new ReentrantLock(true);
        protected final List<String> rawMessages = new ArrayList<>();
        protected final List<String> messages = new ArrayList<>();
        private final PatternLayout patternLayout;

        // Minimal pattern without date, threadid, etc as these are not expected to be useful for tests.
        private static final String PATTERN = "%level %logger %msg%n";

        public MemAppender() {
            // Get the static app-wide context
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            patternLayout = new PatternLayout();
            patternLayout.setContext(lc);
            patternLayout.setPattern(PATTERN);
            patternLayout.start();

            start();
        }

        protected void clear() {
            rawMessages.clear();
            messages.clear();
        }

        @Override
        protected void append(ILoggingEvent e) {
            if (!this.isStarted()) {
                return;
            }

            if (!e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.WARN)) {
                // Filter out INFO and lower; tests are not expected to be interested in such things.
                return;
            }

            this.lock.lock();
            try {
                rawMessages.add(e.getMessage());

                String fm = patternLayout.doLayout(e);
                messages.add(fm);
            } finally {
                this.lock.unlock();
            }
        }
    }
}
