package net.vonos;

import java.util.concurrent.*;

/**
 * Demonstrate the behaviour of method ForkJoinPool.join().
 */
public class ForkJoinDemo {

    public static class NestingTask extends RecursiveAction {
        private final int count;
        NestingTask(int count) {
            this.count = count;
        }

        @Override
        protected void compute() {
            if (count == 0) {
                System.out.println("nthreads:" + this.getPool().getActiveThreadCount());

                Throwable t = new Throwable();
                t.fillInStackTrace();

                StackTraceElement stackInfo[] = t.getStackTrace();
                System.out.println("stack depth:" + stackInfo.length);
                System.out.println(t);
                t.printStackTrace();

            } else {
                NestingTask child = new NestingTask(count-1);
                child.fork();
                child.join();
            }
        }
    }

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool(1);
        NestingTask baseTask = new NestingTask(200);
        pool.invoke(baseTask);
    }
}
