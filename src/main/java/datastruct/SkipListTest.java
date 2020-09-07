package datastruct;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Random;

public class SkipListTest {

    private final Random random = new SecureRandom();

    @Test
    public void test() {
        SkipList skipList = new SkipList();
        skipList.insert("aaa", 123);
        skipList.insert("bbb", 3);
        skipList.insert("ccc", 12);
        assert skipList.nodeNum == 3;

        skipList.printSkipList();

        assert skipList.get("aaa") == 123;
        assert skipList.get("bbb") == 3;
        assert skipList.get("ccc") == 12;

        skipList.insert("aaa", 145);
        assert skipList.get("aaa") == 145;

        Integer removeRet1 = skipList.remove("aaa");
        assert removeRet1 == 145;
        Integer removeRet2 = skipList.remove("aaab");
        assert removeRet2 == null;

        assert skipList.nodeNum == 2;

        skipList.printSkipList();
    }

    @Test
    public void testPerf() {
        SkipList skipList = new SkipList();
        long startTime = System.currentTimeMillis();
        int nodes = 10;
        String[] keys = new String[nodes];
        int[] values = new int[nodes];
        for (int i = 0; i < nodes; i++) {
            String key = random.nextInt(nodes * 100) + "";
            int value = random.nextInt(nodes);
            skipList.insert(key, value);
            keys[i] = key;
            values[i] = value;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("插入" + nodes + "，时间耗时：" + (endTime - startTime) + "");
        skipList.printSkipList();

        for (int i = 0; i < nodes; i++) {
            String key = keys[i];
            int value = values[i];
            assert skipList.get(key) == value;
        }
        long endTime2 = System.currentTimeMillis();
        System.out.println("查询" + nodes + "，时间耗时：" + (endTime2 - endTime) + "");

    }

}
