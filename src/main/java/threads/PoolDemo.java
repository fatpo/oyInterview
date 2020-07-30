package threads;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by ouyang on 2020/7/16.
 */
public class PoolDemo {
    public static void main(String[] args) {
        test1();
    }

    private static void test1() {
        ThreadPoolExecutor executor1 =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        executor1.submit(() -> {
            Thread.sleep(1000);
            return null;
        });
        executor1.submit(() -> {
            Thread.sleep(1000);
            return null;
        });
        executor1.submit(() -> {
            Thread.sleep(1000);
            return null;
        });
        System.out.println("executor1 poolsize " + executor1.getPoolSize());
        System.out.println("executor1 queuesize " + executor1.getQueue().size());
        executor1.shutdown();
    }
}
