package com.huawei.bigdata.spark.examples;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateToBefor {
      static long transformDate( String s) throws ParseException {
              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
              long timeMillis = sdf.parse(s).getTime();
              return  timeMillis;
       }
      static long BeforeTime() {
              Calendar calendar = Calendar.getInstance();
              calendar.add(Calendar.MONTH,-6);

              return  calendar.getTimeInMillis();
       }

}
