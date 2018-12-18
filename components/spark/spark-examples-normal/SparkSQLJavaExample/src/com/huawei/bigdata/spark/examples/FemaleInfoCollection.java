package com.huawei.bigdata.spark.examples;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

import com.huawei.hadoop.security.LoginUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class FemaleInfoCollection {
    public static class FemaleInfo implements Serializable {
        private String name;
        private String gender;
        private Integer stayTime;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Integer getStayTime() {
            return stayTime;
        }

        public void setStayTime(Integer stayTime) {
            this.stayTime = stayTime;
        }
    }

    public static void main(String[] args) throws Exception {
    
        //����Spark��������Ĳ��衣SparkConfig������Spark��Ⱥ�ĸ��ֲ�����setAppName������Wep����ʾӦ�������ѡ�
        SparkConf conf = new SparkConf().setAppName("CollectFemaleInfo");

        //���ڴ�ϵͳ���Լ������á�
        JavaSparkContext jsc = new JavaSparkContext(conf);

        //��Spark 1.x�д���ṹ�����ݣ��к��У�����ڵ㡣
        //��Spark 2.0��ʼ�������滻ΪSparkSession�����ǣ�Ϊ�������ݣ����ǽ����ౣ���ڴ˴���
        SQLContext sqlContext = new org.apache.spark.sql.SQLContext(jsc);

        //���ı�ת��ΪRDD���
        JavaRDD<FemaleInfo> femaleInfoJavaRDD = jsc.textFile(args[0]).map(
                // Function����������һ��String����������һ��FemaleInfo����
                new Function<String, FemaleInfo>() {
                    @Override
                    public FemaleInfo call(String line) throws Exception {

                        //�ԡ�����Ϊ��ʶ���зָ�
                        String[] parts = line.split(",");

                        FemaleInfo femaleInfo = new FemaleInfo();
                        femaleInfo.setName(parts[0]);
                        femaleInfo.setGender(parts[1]);
                        femaleInfo.setStayTime(Integer.parseInt(parts[2].trim()));
                        return femaleInfo;
                    }
                });

        //ע����ӵ�list�д���DataFrame:
        DataFrame schemaFemaleInfo = sqlContext.createDataFrame(femaleInfoJavaRDD,FemaleInfo.class);

        // DataFrameʹ�ø������ƽ���ע��Ϊ��ʱ��
        schemaFemaleInfo.registerTempTable("FemaleInfoTable");

		// ִ��SQL��ѯ������
        DataFrame femaleTimeInfo = sqlContext.sql("select * from " +
                "(select name,sum(stayTime) as totalStayTime from FemaleInfoTable " +
                "where gender = 'female' group by name )" +
                " tmp where totalStayTime >120");

       // �ռ�����е��е��С�
        List<String> result = femaleTimeInfo.javaRDD().map(new Function<Row, String>() {
            public String call(Row row) {
                return  row.getString(0) + "," + row.getLong(1);
            }
        }).collect();
        System.out.println(result);
        jsc.stop();
    }
}
