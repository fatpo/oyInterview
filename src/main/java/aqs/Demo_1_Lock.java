package aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Demo_1_Lock implements Lock {

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    private static class Sync extends AbstractQueuedLongSynchronizer {
        @Override
        protected boolean tryAcquire(long acquires) {
            // 先判断是不是诚心要加锁的
            assert acquires == 1;

            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }

            return false;
        }

        @Override
        protected boolean tryRelease(long releases) {
            // 先判断有没有资格释放
            assert releases == 1;
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }

            // 还原threadId
            setExclusiveOwnerThread(null);

            // 还原状态
            setState(0);

            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

    }

    private static Sync sync = new Sync();


}
