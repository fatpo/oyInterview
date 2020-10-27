package threadlocal;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.LinkedList;
import java.util.List;

public class Demo_PhantomReference {
    private static final List<Object> LIST = new LinkedList<>();
    private static final ReferenceQueue<M> queue = new ReferenceQueue<>();

    public static void main(String[] args) throws IOException {
        PhantomReference<byte[]> phantomReference = new PhantomReference(new M(), queue);

        new Thread(() -> {
            while (true) {
                LIST.add(new byte[1 * 1024 * 1024]);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                System.out.println(phantomReference.get());
            }
        }).start();

        new Thread(() -> {
            while (true) {
                Reference<? extends M> obj = queue.poll();
                if (obj != null) {
                    System.out.println("有个对象被回收了!!!" + obj);
                    break;
                }
            }
        }).start();
    }


}
