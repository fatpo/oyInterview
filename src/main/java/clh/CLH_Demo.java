package clh;

public class CLH_Demo {
    private static volatile int m = 0;

    private static final CLH_Lock lock = new CLH_Lock();

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[100];

        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(() -> {
                lock.lock();
                try {
                    for (int j = 0; j < 10000; j++) {
                        m++;
                    }
                } finally {
                    lock.unlock();
                }
            });
            threads[i] = t;
        }
        for (int i = 0; i < 100; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 100; i++) {
            threads[i].join();
        }

        System.out.println("m = " + m);
    }
}
