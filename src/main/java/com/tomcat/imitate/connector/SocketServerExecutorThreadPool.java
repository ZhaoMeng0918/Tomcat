package com.tomcat.imitate.connector;

import com.tomcat.imitate.startup.Bootstrap;
import org.apache.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName SocketServerExecutorThreadPool
 * @Description 服务处理线程池
 * @Author Administrator
 * @Date 2022-03-31 19:53
 * @Version 1.0.0
 */
public class SocketServerExecutorThreadPool {
    private static Logger logger = Logger.getLogger(Bootstrap.class);

    // 任务队列
    private BlockingQueue<Runnable> taskQueue;

    // 线程集合
    private HashSet<SocketServerClientHandler> workers = new HashSet<>();

    // 核心线程数
    private int coreSize;

    // 获取任务时的超时时间
    private long timeout;

    private TimeUnit timeUnit;

    private RejectPolicy<Runnable> rejectPolicy;

    public SocketServerExecutorThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapcity,
                                          RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.taskQueue = new BlockingQueue<>(queueCapcity);
        this.rejectPolicy = rejectPolicy;
    }

    /**
     * 执行任务
     *
     * @param task
     */
    public void execute(Runnable task) {
        synchronized (workers) {
            if (workers.size() < coreSize) {
                SocketServerClientHandler worker = new SocketServerClientHandler(task);
                workers.add(worker);
                worker.start();
            } else {
                taskQueue.tryPut(rejectPolicy, task);
            }
        }
    }

    // 拒绝策略
    @FunctionalInterface
    interface RejectPolicy<T> {
        void reject(BlockingQueue<T> queue, T task);
    }

    class BlockingQueue<T> {
        // 任务队列
        private Deque<T> queue = new ArrayDeque();

        // 锁
        private ReentrantLock lock = new ReentrantLock();

        // 生产者条件变量
        private Condition fullWaitSet = lock.newCondition();

        // 消费者条件变量
        private Condition emptyWaitSet = lock.newCondition();

        // 容量
        private int capcity;

        public BlockingQueue(int capcity) {
            this.capcity = capcity;
        }

        /**
         * 阻塞获取
         *
         * @return
         */
        public T poll() {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    try {
                        emptyWaitSet.await();
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                T t = queue.removeFirst();
                fullWaitSet.signal();
                return t;
            } finally {
                lock.unlock();
            }
        }

        /**
         * 带超时阻塞获取
         *
         * @param timeout
         * @param unit
         * @return
         */
        public T poll(long timeout, TimeUnit unit) {
            lock.lock();
            try {
                long nanos = unit.toNanos(timeout);
                while (queue.isEmpty()) {
                    try {
                        if (nanos <= 0) return null;
                        nanos = emptyWaitSet.awaitNanos(nanos);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                T t = queue.removeFirst();
                fullWaitSet.signal();
                return t;
            } finally {
                lock.unlock();
            }
        }

        /**
         * 阻塞添加
         *
         * @param task
         */
        public void offer(T task) {
            lock.lock();
            try {
                while (queue.size() == capcity) {
                    try {
                        fullWaitSet.await();
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                queue.addLast(task);
                emptyWaitSet.signal();
            } finally {
                lock.unlock();
            }
        }

        /**
         * 带超时的阻塞添加
         *
         * @param task
         * @param timeout
         * @param timeUnit
         */
        public boolean offer(T task, long timeout, TimeUnit timeUnit) {
            lock.lock();
            try {
                long nanos = timeUnit.toNanos(timeout);
                while (queue.size() == capcity) {
                    try {
                        if (nanos <= 0) return false;
                        nanos = fullWaitSet.awaitNanos(nanos);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                queue.addLast(task);
                emptyWaitSet.signal();
                return true;
            } finally {
                lock.unlock();
            }
        }

        public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
            lock.lock();
            try {
                if (queue.size() == capcity)
                    rejectPolicy.reject(this, task);
                else {
                    queue.addLast(task);
                    emptyWaitSet.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    class SocketServerClientHandler extends Thread {
        private Runnable task;

        public SocketServerClientHandler(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            synchronized (workers) {
                workers.remove(this);
            }
        }

    }
}
