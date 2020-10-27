package threadlocal;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Demo_ThreadLocal_全局共享session {
    static Random random = new SecureRandom();

    public static void main(String[] args) {
        for (; ; ) {
            new Thread(() -> {
                // 请求刚进来，先设置全局session
                Map<String, String> loginMap = new HashMap<>();
                loginMap.put("userId", "fatpo_" + random.nextInt());
                LoginSessionHolder.init(loginMap);

                Recall recall = new Recall();
                recall.haha();

                Rank rank = new Rank();
                rank.heihei();

                Filter filter = new Filter();
                filter.wawa();

                // 请求结束要删除threadLocal
                LoginSessionHolder.clear();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    static class Recall {
        public void haha() {
            System.out.println("haha:" + LoginSessionHolder.get("userId"));
        }
    }

    static class Rank {
        public void heihei() {
            System.out.println("heihei:" + LoginSessionHolder.get("userId"));
        }
    }

    static class Filter {
        public void wawa() {
            System.out.println("wawa: " + LoginSessionHolder.get("userId"));
        }
    }
}


