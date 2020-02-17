package com.kkb;

import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;

public class MyUDF extends UDF {
     public Text evaluate(final Text s) {
         if (null == s) {
             return null;
         }
         //返回大写字母         
         return new Text(s.toString().toUpperCase());
     }
 }