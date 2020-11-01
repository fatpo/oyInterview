package clh;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class CLH_Lock implements Lock {
    private final AtomicReference<Node> tail;
    private final ThreadLocal<Node> preNodeThreadLocal;
    private final ThreadLocal<Node> curNodeThreadLocal;

    private static class Node {
        public volatile boolean isLock; // 必须volatile，否则多线程cache不一致
    }

    public CLH_Lock() {
        tail = new AtomicReference<>(new Node());
        curNodeThreadLocal = ThreadLocal.withInitial(Node::new);
        preNodeThreadLocal = new ThreadLocal<>();
    }

    @Override
    public void lock() {
        // 放到尾巴结点
        Node curNode = curNodeThreadLocal.get();
        curNode.isLock = true;

        Node preNode = tail.getAndSet(curNode);
        preNodeThreadLocal.set(preNode);


        // 等待前结点释放
        while (preNode.isLock) {
        }

        // do something..
        System.out.println("线程：" + Thread.currentThread() + " 拿到锁啦。。。");
    }


    @Override
    public void unlock() {
        // 先释放当前结点
        Node curNode = curNodeThreadLocal.get();
        curNode.isLock = false;

        // 为了防止可能的死锁，要更新一个安全的，没人用的 node结点以备下次lock()使用
        curNodeThreadLocal.set(preNodeThreadLocal.get());

        // do something..
        System.out.println("线程：" + Thread.currentThread() + " 释放锁啦 ===== out ======");

    }

    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }


}
