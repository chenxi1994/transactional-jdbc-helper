package com.dal.generic.impl;

import com.dal.generic.SelectHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by xi.chen on 2016/9/19 16:17.
 */
@Service
public class SelectHelperImpl implements SelectHelper {
    private static final Logger logger = LoggerFactory.getLogger(SelectHelperImpl.class);
    @Resource
    private DataSource dataSource;

    /**
     * @param c
     * @param sql
     * @param params
     * @return
     */
    @Override
    public <T> List<T> select(Class c, String sql, Object... params) {
        List<T> resultList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pst = null;
        ResultSet res = null;
        try {
            //获取连接
            connection = dataSource.getConnection();  //没有事物性，不需要同一个连接
            logger.info("执行的sql语句为：{}", sql);
            pst = connection.prepareStatement(sql);
            for(int i=0;i<params.length;i++){
                pst.setObject(i + 1, params[i]);
            }
            res = pst.executeQuery();
            //获取对应table的字段，没有id字段
            Map<Field,String> fieldMap= getTableFields(c);
            while (res.next()) {

                T modle = (T) c.newInstance();
                Set<Map.Entry<Field, String>> entrySet= fieldMap.entrySet();
                for (Map.Entry<Field,String> entry: entrySet) {
                    Field field = entry.getKey();
                    field.setAccessible(true);
                    field.set(modle,res.getObject(entry.getValue()));
                }
                resultList.add(modle);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != pst) {
                    pst.close();
                }
                if (null != connection) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }

    /**
     * 查询需要的字段.不是select * form ...
     * @param sql
     * @param params
     * @return
     */
    public List<Map<String, Object>> select(String sql, Object... params) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pst = null;
        ResultSet res = null;
        try {
            //获取连接
            connection = dataSource.getConnection();  //没有事物性，不需要同一个连接
            logger.info("执行的sql语句为：{}", sql);
            pst = connection.prepareStatement(sql);
            for(int i=0;i<params.length;i++){
                pst.setObject(i + 1, params[i]);
            }
            res = pst.executeQuery();
            String[] sqlfields =  getSqlfields(sql);
            while (res.next()) {

                Map<String,Object> map = new HashMap<>();
                for(int i=0;i<sqlfields.length;i++){
                    map.put(sqlfields[i],res.getObject(sqlfields[i]));
                }
                resultList.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }  finally {
            try {
                if (null != pst) {
                    pst.close();
                }
                if (null != connection) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }


    /**
     *
     * @param c
     * @return
     */
    private Map<Field,String> getTableFields(Class<?> c) {
        Map<Field,String> map = new HashMap<>();
        //获取所有的字段
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Id id = fields[i].getAnnotation(Id.class);
            if(null != id){ //ID特殊处理
                Column column = fields[i].getAnnotation(Column.class);
                if(null !=column){ //如果不是主键id

                    //对应数据库中的字段
                    String name = column.name();
                    if (StringUtils.isNotBlank(name)) {
                        map.put(fields[i],name);
                    }else {
                        map.put(fields[i],fields[i].getName());
                    }
                }else {
                    map.put(fields[i],fields[i].getName());

                }
                continue;
            }
            Column column = fields[i].getAnnotation(Column.class);
            if (null != column) {
                //对应数据库中的字段
                String name = column.name();
                if (StringUtils.isNotBlank(name)) {
                    map.put(fields[i],name);
                }
            }
        }
        return map;
    }

    /**
     * 解析查询sql
     */
    private String[] getSqlfields(String sql){
        String upSql = sql.toUpperCase();
        String fieldStr = upSql.substring("SELECT".length(),upSql.indexOf("FROM"));
        String[] splits = fieldStr.trim().split(",");
        for(int i=0;i<splits.length;i++){
            splits[i] = splits[i].trim();
        }
        return splits;
    }

    public static void main(String[] args) {
        String sql = "SELECT id, sfsf ,    sfs FROM t_payment_notice WHERE notice_success ";
        String upSql = sql.toUpperCase();
        String fieldStr = upSql.substring("SELECT".length(),upSql.indexOf("FROM"));
        System.out.println(fieldStr);
        String[] splits  = fieldStr.split(",");
        for(int i=0;i<splits.length;i++){
            splits[i] = splits[i].trim();
        }

        System.out.println(Arrays.asList(splits));
    }
}
