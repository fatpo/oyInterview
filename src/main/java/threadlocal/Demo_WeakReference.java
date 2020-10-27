package threadlocal;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class Demo_WeakReference {
    public static void main(String[] args) throws IOException {
        WeakReference<byte[]> weakReference = new WeakReference(new byte[10 * 1024 * 1024]);
        System.out.println(weakReference.get());
        System.gc();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(weakReference.get());

        // 因为设置了 -Xmx20m 所以整个JVM堆内存顶多20M，之前已经申请过10M了，再申请13M 肯定会OOM
        // 但是gc后它会立马释放弱引用指向的内存，所以就避免了OOM
        byte[] value2 = new byte[13 * 1024 * 1024];
        System.out.println(weakReference.get());
    }


}
