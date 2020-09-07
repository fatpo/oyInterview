package datastruct;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Random;

public class SkipList {
    int curHeight;
    int nodeNum;
    SkipNode head;
    SkipNode tail;
    Random random;


    @Test
    public void test() {
        SkipList skipList = new SkipList();
        skipList.insert("aaa", 123);
        skipList.insert("bbb", 3);
        skipList.insert("ccc", 12);

        skipList.printSkipList();

        assert skipList.get("aaa") == 123;
        assert skipList.get("bbb") == 3;
        assert skipList.get("ccc") == 12;

        skipList.insert("aaa", 145);
        assert skipList.get("aaa") == 145;
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
            System.out.println(key + " " + value);
            assert skipList.get(key) == value;
        }
        long endTime2 = System.currentTimeMillis();
        System.out.println("查询" + nodes + "，时间耗时：" + (endTime2 - endTime) + "");

    }

    public SkipList() {
        curHeight = 0;
        nodeNum = 0;
        random = new SecureRandom();

        SkipNode p1 = new SkipNode(SkipNode.minPos, null);
        SkipNode p2 = new SkipNode(SkipNode.maxPos, null);
        p1.right = p2;
        p2.left = p1;

        head = p1;
        tail = p2;
    }

    public void printSkipList() {
        SkipNode x = head;
        SkipNode x2 = head;
        assert x2 != null;

        while (true) {
            while (x != null) {
                System.out.print(x.key + " ");
                x = x.right;
            }
            System.out.println();
            if (x2.down != null) {
                x = x2.down;
                x2 = x2.down;
            } else {
                break;
            }
        }
    }

    public SkipNode findNode(String key) {
        SkipNode x = head;
        while (true) {
            while (!x.right.key.equals(SkipNode.maxPos) && key.compareTo(x.right.key) <= 0) {
                x = x.right;
            }
            if (x.down != null) {
                x = x.down;
            } else {
                break;
            }
        }
        return x;
    }

    public Integer get(String key) {
        SkipNode x = findNode(key);
        if (x.key.equals(key)) {
            return x.value;
        }
        return null;
    }

    public void insert(String key, Integer value) {
        SkipNode foundNode = findNode(key);
        // 如果有相同的key，则更新value即可
        if (key.equals(foundNode.key)) {
            foundNode.value = value;
            return;
        }

        // 新建一个node，维护最底层的链表
        SkipNode newNode = new SkipNode(key, value);
        newNode.left = foundNode;
        newNode.right = foundNode.right;
        foundNode.right.left = newNode;
        foundNode.right = newNode;

        int height = 0;
        while (random.nextDouble() > 0.5) {
            System.out.println("key:" + key + " 运气好，往上跳一层，当前层数: " + height);
            // 层数+1
            height++;

            // 确保整体有这么高的楼层
            if (height > curHeight) {
                System.out.println("申请一个新层，整个跳表的高度：" + curHeight);
                makeNewLevel();
            }

            // 往前找到一个具备通往高层链表的节点
            while (foundNode.up == null) {
                foundNode = foundNode.left;
            }
            foundNode = foundNode.up;

            // 这一层也要创建一个新的节点
            SkipNode x = new SkipNode(key, null);
            x.left = foundNode;
            x.right = foundNode.right;
            foundNode.right.left = x;
            foundNode.right = x;

            x.down = newNode;
            newNode.up = x;

            newNode = x;
        }

    }

    private void makeNewLevel() {
        SkipNode p1 = new SkipNode(SkipNode.minPos, null);
        SkipNode p2 = new SkipNode(SkipNode.maxPos, null);

        p1.down = head;
        p2.down = tail;
        head.up = p1;
        tail.up = p2;

        p1.right = p2;
        p2.left = p1;

        head = p1;
        tail = p2;

        // 楼层+1
        curHeight++;
    }


}


class SkipNode {
    String key;
    Integer value;

    SkipNode left;
    SkipNode right;
    SkipNode up;
    SkipNode down;

    public SkipNode(String key, Integer value) {
        this.key = key;
        this.value = value;
    }

    public final static String minPos = "-∞";
    public final static String maxPos = "+∞";
}