package datastruct;

import java.security.SecureRandom;
import java.util.Random;

public class SkipList {
    int curHeight;
    int nodeNum;
    SkipNode head;
    SkipNode tail;
    Random random;


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
        System.out.println("开始打印跳跃表...");
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

    /**
     * 核心函数：
     * 1、从最高层开始往right方向找；
     * 2、打不过了就往下走一层；
     * 3、循环1，2步骤直到最后一层。
     *
     * @param key 待查找的key
     * @return <= key 的那个结点
     */
    public SkipNode findNode(String key) {
        SkipNode x = head;
        while (true) {
            while (!x.right.key.equals(SkipNode.maxPos) && key.compareTo(x.right.key) >= 0) {
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

        this.nodeNum++;
    }

    /**
     * 创一个新层
     */
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

    public Integer remove(String key) {
        SkipNode node = findNode(key);
        if (!key.equals(node.key)) {
            System.out.println("node: " + key + " not exist!");
            return null;
        }

        int oldValue = node.value;

        // 删除高层节点，如果有的话
        while (node != null) {
            node.left.right = node.right;
            node.right.left = node.left;

            node = node.up;
        }

        // 总体结点数量 - 1
        this.nodeNum--;

        return oldValue;
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