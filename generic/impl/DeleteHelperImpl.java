package com.msxf.psp.dal.generic.impl;

import com.msxf.psp.dal.generic.DeleteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xi.chen on 2016/9/19 18:35.
 */
@Service
public class DeleteHelperImpl implements DeleteHelper {
    private static final Logger logger = LoggerFactory.getLogger(DeleteHelperImpl.class);
    //当前系统采用的是ali的数据源
    @Resource
    private DataSource dataSource;

    /**
     * 通过list的id删除数据
     *
     * @param list
     * @return
     */
    @Transactional
    @Override
    public int[] delete(List<?> list) throws SQLException, IllegalAccessException {

        int size = list.size();
        if (size < 1) {
            return null;
        }
        Object obj = list.get(0);
        int[] result = new int[size]; //返回没条数据删除成功的code
        Connection connection = null;
        PreparedStatement pst = null;
        ResultSet res = null;
        try {
            //获取连接
            connection = DataSourceUtils.getConnection(dataSource);  //这个是支持事物的核心
            //connection.setAutoCommit(false);

            List<Field> idFieldList = getIdFields(obj.getClass());
            String sql = getDeleteSqlById(obj.getClass(),idFieldList);
            logger.info("执行的sql语句为：{}", sql);
            pst = connection.prepareStatement(sql);


            for (int i = 0; i < list.size(); i++) { //遍历list数据
                for(int j= 0;j<idFieldList.size();j++){ //遍历主键字段
                    Object value =  idFieldList.get(j).get(list.get(i));
                    pst.setObject((j + 1), value);
                }

                pst.addBatch();
            }

            result = pst.executeBatch();
            //connection.commit();
            // pst.clearBatch();

        } catch (SQLException e) {
          /*  try {
                if (null != connection) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }*/
            throw new SQLException(e);
        }/*finally {
            try {
                if (null != pst) {
                    pst.close();
                }
                if (null != connection) {
                    connection.close();
                }
            } catch ( e) {
                e.printStackTrace();
            }
        }*/
        return result;
    }

    @Transactional
    @Override
    public void delete(String sql, Object... param) throws SQLException {
        Connection connection = null;
        PreparedStatement pst = null;
        ResultSet res = null;
        try {
            //获取连接
            connection = DataSourceUtils.getConnection(dataSource);  //这个是支持事物的核心
            logger.info("执行的sql语句为：{}", sql);
            pst = connection.prepareStatement(sql);
            for (int i = 0; i < param.length; i++) {
                pst.setObject((i+1), param[i]);
            }
            pst.execute();
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }

    /**
     * 获取主键id对应model的字段
     *
     * @param c
     * @return
     */
    private  List<Field> getIdFields(Class<?> c) throws SQLException {
        List<Field> idFieldList = new ArrayList<>();
        //获取所有的字段
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            Id id = fields[i].getAnnotation(Id.class);
            if (null != id) {
                idFieldList.add(fields[i]);
            }
        }
        if (idFieldList.size() == 0) {
            throw new SQLException("找不到主键id");
        }
        return idFieldList;
    }

    /**
     * 获取通过id删除的sql语句
     *
     * @param c
     * @return
     */
    private String getDeleteSqlById(Class<?> c, List<Field> idFieldList) {
        StringBuffer sb = new StringBuffer("DELETE FROM ");
        //获取表名
        Table table = c.getAnnotation(Table.class);
        String tableName = table.name();
        sb.append(tableName).append(" WHERE ");
        for(int i=0;i<idFieldList.size();i++){
            Column column =idFieldList.get(i).getAnnotation(Column.class);
            String fieldName ="";
            if(null != column){
                fieldName = column.name();
            }else {
                fieldName = idFieldList.get(i).getName();
            }
            sb.append(fieldName).append(" = ? ").append("AND ");
        }
        //去掉最后一个AND
        String deleteSql = sb.toString();
        deleteSql = deleteSql.substring(0,deleteSql.lastIndexOf("AND")).trim();
        return  deleteSql;
    }

}
