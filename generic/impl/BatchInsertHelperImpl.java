package com.dal.generic.impl;

import com.dal.generic.BatchInsertHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xi.chen on 2016/8/25 13:05.
 */
@Service
public class BatchInsertHelperImpl implements BatchInsertHelper {
    private static final Logger logger = LoggerFactory.getLogger(BatchInsertHelperImpl.class);

    @Resource
    private DataSource dataSource;

    /**
     *
     * @param list       数据
     * @param isIgnore   是否忽略重复，根据唯一索引判断，false不忽略，如果违背唯一索引原则就抛异常；
     *                   true忽略重复，将重复的数据去掉不插入库中。
     * @return int[] 批量插入返回结果，1表示插入成功，0表示忽略;
     */
    @Override
    @Transactional
    public int[] bacthInsert(List<?> list) throws NoSuchFieldException, IllegalAccessException, SQLException {
        return bacthInsert(list,false, "");
    }

    /**
     * @param list
     * @param isIgnore
     * @param tableName
     * @return
     */
    @Transactional
    @Override
    public int[] bacthInsert(List<?> list, boolean isIgnore, String tableName) throws SQLException, NoSuchFieldException, IllegalAccessException {
        int size = list.size();
        if (size < 1) {
            return null;
        }
        int[] result = new int[size];
        Object t = list.get(0);
        Connection connection = null;
        PreparedStatement pst = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);  //确保获取的是同一个连接????加上@Transactional了好像不需要
            //connection = dataSource.getConnection();
             //connection.setAutoCommit(false);
            String sql = getInsertSql(t, isIgnore, tableName);
            logger.info("执行的sql语句为：{}", sql);
            pst = connection.prepareStatement(sql);
            List<String> list1 = getFieds(t.getClass());
            for (int i = 0; i < list.size(); i++) {
                Object obj = list.get(i);
                Class c = obj.getClass();
                for (int j = 0; j < list1.size(); j++) {
                    Field field = c.getDeclaredField(list1.get(j));
                    field.setAccessible(true);
                    pst.setObject(j + 1, field.get(obj));
                }
                pst.addBatch();
             /*   if (i>0 && i % commitSize == 0) {
                    int[] resutCode = pst.executeBatch();
                    if(isIgnore){
                        System.arraycopy(resutCode,0,result,i-1000,resutCode.length);
                    }
                    connection.commit();
                    pst.clearBatch();
                }*/
            }
            result = pst.executeBatch();
           /* if(isIgnore){
                System.arraycopy(resutCode,0,result,size-resutCode.length,resutCode.length);
            }*/
            //connection.commit();
            //pst.clearBatch();

        } catch (SQLException e) {
          /*  try {
                if (null != connection) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }*/
            throw new SQLException(e);
        } /*finally {
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
        }*/
        return result;
    }

    @Transactional
    @Override
    public int[] bacthInsert(List<?> list, boolean isIgnore) throws SQLException, NoSuchFieldException, IllegalAccessException {
        return bacthInsert(list,isIgnore, "");
    }

    /**
     * 指定表名批量插入
     *
     * @param list
     * @param tableName
     */
    @Override
    @Transactional
    public void bacthInsertSpecifiedTableName(List<?> list, String tableName) throws NoSuchFieldException, IllegalAccessException, SQLException {
        bacthInsert(list, false, tableName);
    }

    /**
     * 拼接insert sql语句
     *
     * @return
     */
    private <T> String getInsertSql(T t, Boolean isIgnore, String tableName) {
        String handle = "INSERT INTO";
        if (isIgnore) {
            handle = "INSERT IGNORE INTO";
        }
        StringBuilder insertSql = new StringBuilder(handle);
        Class<?> aClass = t.getClass();
        //获取字段的数组
        List<String> fiedsList = getTableFieds(aClass);

        //获取表名
        Table table = aClass.getAnnotation(Table.class);
        if (null == table) {
            return insertSql.toString();
        }
        if (StringUtils.isBlank(tableName)) {
            tableName = table.name();
        }

        insertSql.append(" `").append(tableName).append("` ").append("(");
        //添加字段名
        for (int i = 0; i < fiedsList.size(); i++) {
            insertSql.append("`").append(fiedsList.get(i)).append("`").append(",");
        }
        //删除最后一个“,”
        if (insertSql.lastIndexOf(",") == insertSql.length() - 1) {
            insertSql.deleteCharAt(insertSql.length() - 1);

        }
        // insertSql.deleteCharAt(insertSql.length()-1);
        insertSql.append(")").append(" VALUE ").append("(");

        //添加value的占位符
        for (int i = 0; i < fiedsList.size(); i++) {
            insertSql.append("?").append(",");
        }
        //删除最后一个“,”
        if (insertSql.lastIndexOf(",") == insertSql.length() - 1) {
            insertSql.deleteCharAt(insertSql.length() - 1);
        }
        insertSql.append(");");
        return insertSql.toString();
    }


    /**
     * 获取一个类的泛型类型的数组
     * 现在有一个问题，这个方式获取不了类的泛型的类型,好像是泛型在运行的时候获取不了它的类型
     *
     * @param c
     * @return
     */
    private Type[] getGenericType(Class<?> c) {
        Type genType = c.getGenericSuperclass();
        Type[] types = null;
        //判断当前类型是否是参数化类型，如：xxx<T...>
        if (genType instanceof ParameterizedType) {
            types = ((ParameterizedType) genType).getActualTypeArguments();
        }
        return types;
    }

    /**
     * 获取类中需要映射到数据表中的字段的list集合，注：id主键除外。
     * 这里支持jpa entity注解的字段。
     *
     * @param c
     * @return
     */
    private List<String> getTableFieds(Class<?> c) {
        List<String> list = new ArrayList<>();
        //获取所有的字段
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column column = fields[i].getAnnotation(Column.class);
            if (null != column) {
                //对应数据库中的字段
                String name = column.name();
                if (StringUtils.isNotBlank(name)) {
                    list.add(name);
                }
            }

        }
        return list;
    }

    /**
     * 或去类的所有字段，除了id
     * 这里支持jpa entity注解的字段。
     *
     * @param c
     * @return
     */
    private List<String> getFieds(Class<?> c) {
        List<String> list = new ArrayList<>();
        //获取所有的字段
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Column column = fields[i].getAnnotation(Column.class);
            if (null != column) {
                list.add(fields[i].getName());
            }
        }
        return list;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        Type genType = list.getClass().getGenericSuperclass();
        Type[] types = null;
        //判断当前类型是否是参数化类型，如：xxx<T...>
        if (genType instanceof ParameterizedType) {
            types = ((ParameterizedType) genType).getActualTypeArguments();
        }
    }
}
