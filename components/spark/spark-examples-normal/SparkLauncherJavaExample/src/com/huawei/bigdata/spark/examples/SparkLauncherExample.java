package com.huawei.bigdata.spark.examples;

import org.apache.spark.launcher.SparkLauncher;

/**
  * Submit spark app.
  * args(0) is the mode to run spark app, eg yarn-client
  * args(1) is the path of spark app jar
  * args(2) is the main class of spark app
  * args(3...) is the parameters of spark app
  */
public class SparkLauncherExample {
    public static void main(String[] args) throws Exception {
        System.out.println("com.huawei.bigdata.spark.examples.SparkLauncherExample <mode> <jarParh> <app_main_class> <appArgs>");

        //ʹ�ô����Ա�̷�ʽ����SparkӦ�ó��򡣸���ʹ�ù�����ģʽ����ͻ�������SparkӦ�ó��򲢽�����Ϊ�ӽ���������
        SparkLauncher launcher = new SparkLauncher();
        //ΪӦ�ó�������Spark����������
        launcher.setMaster(args[0])//ΪӦ�ó�������Spark����������
            .setAppResource(args[1]) //������Ӧ�ó�����Դ��
            .setMainClass(args[2]);//����Java / ScalaӦ�ó����Ӧ�ó��������ơ�
        if (args.length > 3) {
            String[] list = new String[args.length - 3];//���ü��ϵĴ�С
            for (int i = 3; i < args.length; i++) {
                //��ȥǰ��λ������Ĵ浽list��
                list[i-3] = args[i];
            }
            // ����Ӧ�ó���args
            launcher.addAppArgs(list);
        }

        // ���������������õ�SparkӦ�ó���������̡�
        Process process = launcher.launch();
        //��ȡSpark����������־
        new Thread(new ISRRunnable(process.getErrorStream())).start();
        int exitCode = process.waitFor();//���̵ȴ�
        System.out.println("Finished! Exit code is "  + exitCode);
    }
}

