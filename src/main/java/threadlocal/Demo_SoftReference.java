package threadlocal;

import java.io.IOException;
import java.lang.ref.SoftReference;

public class Demo_SoftReference {
    public static void main(String[] args) throws IOException {
        SoftReference<byte[]> softReference = new SoftReference(new byte[10 * 1024 * 1024]);
        System.out.println(softReference.get());
        System.gc();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(softReference.get());

        // 因为设置了 -Xmx20m 所以整个JVM堆内存顶多20M，之前已经申请过10M了，再申请13M 肯定会OOM
        // 但是它会释放软引用指向的内存，所以就避免了OOM
        byte[] value2 = new byte[13 * 1024 * 1024];
        System.out.println(softReference.get());
    }

    private void useForSoftReference() {
        /*
        使用场景：用于哪些大对象的缓存，比如图片、浏览的页面

        // 获取页面进行浏览
        Browser prev = new Browser();

        // 浏览完毕后置为软引用
        SoftReference sr = new SoftReference(prev);

        if(sr.get() != null) {
            // 还没有被回收器回收，直接获取
            rev = (Browser) sr.get();
        } else {
            // 由于内存吃紧，软引用的对象被回收了
            // 这时重新构建前一页面
            prev = new Browser();
            sr = new SoftReference(prev);
        }
        */
    }
}
