package net.vonos.moi.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RabbitMQ {

    // Adjust this to point to your RabbitMQ server
    public static final String rmqServerUrl="amqp://user:password@rmqhost:rmqport/rmqenv";

    public static final int N_MSGS = 20;

    public static void main(String[] args) throws Exception {
        testGetter();
        // testSubscribers();
    }

    private static void testGetter() throws Exception {

        ThreadFactory threadFactory = new ThreadFactory() {
            int idx;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("RMQ-" + (++idx));
                t.setDaemon(true);
                return t;
            }
        };

        ExecutorService connectionExecutor = Executors.newFixedThreadPool(5, threadFactory);
        ConnectionFactory cf = new ConnectionFactory();
        cf.setUri(rmqServerUrl);
        Connection con = cf.newConnection(connectionExecutor);

        // Create an anonymous queue
        Channel setupChannel = con.createChannel();
        String queue = setupChannel.queueDeclare().getQueue();
        int prefetchSizeBytes = 0; // max message-body-size that will be "pushed" from server to client - but not yet supported 
        int prefetchCount = 13; // max number of messages that will be "pushed" with outstanding acks
        boolean global = true; // true: limit is per channel, false: limit is per consumer
        setupChannel.basicQos(prefetchSizeBytes, prefetchCount, global);

        // Write some messages to the queue
        Channel producerChannel = con.createChannel();
        for(int i=0; i<N_MSGS; ++i) {
            String message = "fast-message " + i;
            producerChannel.basicPublish("", queue, MessageProperties.BASIC, message.getBytes());
        }

        System.out.println("\n\n====\n\n");

        Channel getterChannel = con.createChannel();
        for(int i=0; i<N_MSGS; ++i) {
            GetResponse rsp1 = getterChannel.basicGet(queue, false);
            String txt = new String(rsp1.getBody());
            System.out.println("got:" + txt);
        }

        con.close();
    }

    private static void testSubscribers() throws Exception {

        ThreadFactory threadFactory = new ThreadFactory() {
            int idx;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("RMQ-" + (++idx));
                t.setDaemon(true);
                return t;
            }
        };

        ExecutorService connectionExecutor = Executors.newFixedThreadPool(5, threadFactory);
        ConnectionFactory cf = new ConnectionFactory();
        cf.setUri(rmqServerUrl);
        Connection con = cf.newConnection(connectionExecutor);

        // Create an anonymous queue
        Channel setupChannel = con.createChannel();
        String queue = setupChannel.queueDeclare().getQueue();
        int prefetchSizeBytes = 0; // max message-body-size that will be "pushed" from server to client - but not yet supported 
        int prefetchCount = 13; // max number of messages that will be "pushed" with outstanding acks
        boolean global = true; // true: limit is per channel, false: limit is per consumer
        setupChannel.basicQos(prefetchSizeBytes, prefetchCount, global);

        Counter counter = new Counter(N_MSGS);

        // Set up 3 listeners all with their own channels
        MyConsumer c1 = new MyConsumer("c1", con.createChannel(), counter);
        c1.subscribe(queue);

        MyConsumer c2 = new MyConsumer("c2", con.createChannel(), counter);
        c2.subscribe(queue);

        MyConsumer c3 = new MyConsumer("c3", con.createChannel(), counter);
        c3.subscribe(queue);

        Channel channel4 = con.createChannel();
        MyConsumer c4a = new MyConsumer("c4a", channel4, counter);
        c4a.subscribe(queue);
        MyConsumer c4b = new MyConsumer("c4b", channel4, counter);
        c4b.subscribe(queue);

        // Write some messages to the queue
        Channel producerChannel = con.createChannel();
        for(int i=0; i<N_MSGS; ++i) {
            String message = "fast-message " + i;
            producerChannel.basicPublish("", queue, MessageProperties.BASIC, message.getBytes());
        }

        counter.waitForZero();

        System.out.println("\n\n====\n\n");

        counter.reset(10);
        for(int i=0; i<10; ++i) {
            String message = "slow-message " + i;
            producerChannel.basicPublish("", queue, MessageProperties.BASIC, message.getBytes());
            Thread.sleep(2000); // wait 2 seconds
        }

        counter.waitForZero();

        con.close();
    }

    static class Counter {
        int count;

        Counter(int count) {
            this.count = count;
        }

        synchronized void dec() {
            --count;
            if (count == 0) {
                this.notify();
            }
        }

        synchronized void reset(int count) {
            this.count = count;
        }

        void waitForZero() throws InterruptedException {
            synchronized (this) {
                if (count != 0) {
                    this.wait();
                }
            }
        }
    }

    static class MyConsumer extends DefaultConsumer {
        final String name;
        final Counter latch;
        String consumerTag;

        MyConsumer(String name, Channel channel, Counter latch) {
            super(channel);
            this.name = name;
            this.latch = latch;
        }

        void subscribe(String queue) throws IOException {
            if (consumerTag != null) {
                throw new IllegalStateException("Cannot be called multiple times");
            }
            // basicConsume:
            //   String queue, boolean autoAck, String consumerTag,
            //   boolean noLocal, boolean exclusive,
            //   Map<String, Object> arguments,
            //   callback

            // consume with autoAck set to false
            consumerTag = getChannel().basicConsume(queue, false, "", false, false, null, this);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            String msg = String.format(
                "%s: thread %s received message with tag %s and content %s",
                name, Thread.currentThread().getName(), consumerTag, new String(body));

            System.out.println("Begin: " + msg);
            try {
                Thread.sleep((int) (Math.random() * 1000)); // wait up to 1second
            } catch(InterruptedException e) {
                throw new IOException("Interrupted");
            } finally {
                getChannel().basicAck(envelope.getDeliveryTag(), false);
                System.out.println("End: " + msg);
                latch.dec();
            }
        }
    }
}

