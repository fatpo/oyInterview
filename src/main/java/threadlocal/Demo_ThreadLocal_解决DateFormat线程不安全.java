package threadlocal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Demo_ThreadLocal_解决DateFormat线程不安全 {
    static ThreadLocal<DateFormat> dateFormatThreadLocal = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static void main(String[] args) {
        Date date = null;
        try {
            date = dateFormatThreadLocal.get().parse("2020-10-10 12:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(date);
    }


}


