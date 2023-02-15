// Author Simon Kitching 2017.
// This software is in the public domain
package net.vonos.junit.rules;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JUnit rule which creates a temporary directory (accessible via method getDir), runs a test, then deletes
 * the directory.
 */
public class WorkdirRule implements TestRule {
    private final String prefix;
    private Path dir;

    public WorkdirRule(String prefix) {
        this.prefix = prefix;
    }

    public Path getDir() {
        return dir;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                dir = Files.createTempDirectory(prefix);
                try {
                    statement.evaluate();
                } finally {
                    FileUtils.deleteDirectory(dir.toFile());
                }
            }
        };
    }
}
