package aqs;

public class Demo_1 {

    private static Demo_1_Lock lock = new Demo_1_Lock();

    private static int m = 0;

    public static void main(String[] args) throws Exception {
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

        System.out.println("m: " + m);
        Thread.sleep(1000);

    }

}
