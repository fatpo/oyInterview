package threadlocal;

public class Demo_ThreadLocal_共享变量 {
    static ThreadLocal<String> userId = new ThreadLocal<>();

    public static void main(String[] args) {
        userId.set("fatpo!");
        Recall recall = new Recall();
        Rank rank = new Rank();
        Filter filter = new Filter();

        recall.haha();
        rank.heihei();
        filter.wawa();
    }

    static class Recall {
        public void haha() {
            System.out.println("haha:" + userId.get());
        }
    }

    static class Rank {
        public void heihei() {
            System.out.println("heihei:" + userId.get());
        }
    }

    static class Filter {
        public void wawa() {
            System.out.println("wawa" + userId.get());
        }
    }
}


