package com.huawei.bigdata.spark.examples;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import scala.Tuple2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.util.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.*;
import com.huawei.hadoop.security.LoginUtil;


/**
 * calculate data from hbase1/hbase2,then update to hbase2
 */
public class SparkHbasetoHbase {

  public static void main(final String[] args) throws Exception {
  
    //����Spark��������Ĳ��衣
    //setAppName������web����ʾӦ������
    SparkConf conf = new SparkConf().setAppName("SparkHbasetoHbase");
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");//���л�
    conf.set("spark.kryo.registrator", "com.huawei.bigdata.spark.examples.MyRegistrator");

    //���ڴ�ϵͳ���Լ������á�
    JavaSparkContext jsc = new JavaSparkContext(conf);

    // Create the configuration parameter to connect the HBase. The hbase-site.xml must be included in the classpath.
    Configuration hbConf = HBaseConfiguration.create(jsc.hadoopConfiguration());

    // Hbase������Ķ���
    Scan scan = new org.apache.hadoop.hbase.client.Scan();

    //ָ����Ҫ���������壬���Ϊ�գ��򷵻����е���
    scan.addFamily(Bytes.toBytes("cf"));//colomn family

    //�����л�����ɨ����ת�����ַ�������
    org.apache.hadoop.hbase.protobuf.generated.ClientProtos.Scan proto = ProtobufUtil.toScan(scan);

    String scanToString = Base64.encodeBytes(proto.toByteArray());
    //��ҵ������ָ�������
    hbConf.set(TableInputFormat.INPUT_TABLE, "table1");//table name
    //Base-64����ɨ���ǡ�
    hbConf.set(TableInputFormat.SCAN, scanToString);

    //ͨ��Spark�ӿڻ�ȡ���е�����
    //��ȡ���ݲ�ת����rdd TableInputFormat �� org.apache.hadoop.hbase.mapreduce ���µġ����hbase��ѯ���Result
    JavaPairRDD rdd = jsc.newAPIHadoopRDD(hbConf, TableInputFormat.class, ImmutableBytesWritable.class, Result.class);

    // ����hbase table1���е�ÿһ��partition, Ȼ����µ�Hbase table2��
    // ��������������٣�Ҳ����ʹ��rdd.foreach()����
    rdd.foreachPartition(//������
      new VoidFunction<Iterator<Tuple2<ImmutableBytesWritable, Result>>>() {
		  //Tuple2�൱��һ�������������ŵ��ǲ�ѯ����Ľ������������Ϳ��ܲ�һ�¡�����ͨ��_1(),_2()�����е���
        public void call(Iterator<Tuple2<ImmutableBytesWritable, Result>> iterator) throws Exception {
          hBaseWriter(iterator);//����д��ķ���
        }
      }
    );

    jsc.stop();
  }

  /**
   * write to table2 in exetutor
   *
   * @param iterator partition data from table1
   */
  private static void hBaseWriter(Iterator<Tuple2<ImmutableBytesWritable, Result>> iterator) throws IOException {
    //read hbase
    String tableName = "table2";
    String columnFamily = "cf";
    String qualifier = "cid";
    Configuration conf = HBaseConfiguration.create();//ʹ��HBase��Դ��������
    Connection connection = null;
    Table table = null;
    try {
      connection = ConnectionFactory.createConnection(conf);//�������ô���һ��Connection����
      table = connection.getTable(TableName.valueOf(tableName));//�������ڷ��ʱ��Tableʵ�֡�

      List<Get> rowList = new ArrayList<Get>();
      List<Tuple2<ImmutableBytesWritable, Result>> table1List = new ArrayList<Tuple2<ImmutableBytesWritable, Result>>();
	  //���е������Ѵ�table1�����ݷŵ�list�С�
      while (iterator.hasNext()) {
        Tuple2<ImmutableBytesWritable, Result> item = iterator.next();
        Get get = new Get(item._2().getRow());//������ݵĵڶ���ֵ��resulet��Ӧ��ֵ
        table1List.add(item);
        rowList.add(get);
      }

      //��table2�л������
      Result[] resultDataBuffer = table.get(rowList);
      List<Put> putList = new ArrayList<Put>();
      for (int i = 0; i < resultDataBuffer.length; i++) {
		//����table2��ÿ������
        Result resultData = resultDataBuffer[i]; 
		//�п�
        if (!resultData.isEmpty()) {
          //query hbase1Value
          String hbase1Value = "";
		  //����table1ȡ���������ݷŵ��������У����б���
          Iterator<Cell> it = table1List.get(i)._2().listCells().iterator();
          while (it.hasNext()) {
            Cell c = it.next();
            // ͨ����������η�ȥ���table1��ֵ
            if (columnFamily.equals(Bytes.toString(CellUtil.cloneFamily(c)))
              && qualifier.equals(Bytes.toString(CellUtil.cloneQualifier(c)))) {
              hbase1Value = Bytes.toString(CellUtil.cloneValue(c));
            }
          }

          String hbase2Value = Bytes.toString(resultData.getValue(columnFamily.getBytes(), qualifier.getBytes()));
		  //�������put��������������ʵ����put
          Put put = new Put(table1List.get(i)._2().getRow());

          //������ֵ
          int resultValue = Integer.parseInt(hbase1Value) + Integer.parseInt(hbase2Value);
          //��������
          put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier), Bytes.toBytes(String.valueOf(resultValue)));
          putList.add(put);
        }
      }

      if (putList.size() > 0) {
        table.put(putList);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (table != null) {
        try {
          table.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (connection != null) {
        try {
          // Close the HBase connection.
          connection.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
