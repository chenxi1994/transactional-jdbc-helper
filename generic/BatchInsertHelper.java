package com.dal.generic;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by xi.chen on 2016/8/25 13:01.
 */
public interface BatchInsertHelper {

    /**
     * 采用jdbc批量插入方式
     * @param list 数据
     */
    public int[] bacthInsert(List<?> list) throws NoSuchFieldException, IllegalAccessException, SQLException;

    /**
     *
     * @param list
     * @param isIgnore 是否忽略重复，false-不忽略  true-忽略
     * @param tableName
     * @return
     * @throws SQLException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public int[] bacthInsert(List<?> list, boolean isIgnore, String tableName) throws SQLException, NoSuchFieldException, IllegalAccessException;

    /**
     *
     * @param list
     * @param isIgnore 是否忽略重复,默认为false,false - 不忽略，true - 忽略
     *                 忽略的情况下，如果存在唯一索引，测插入不成功。
     * @return
     * @throws SQLException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public int[] bacthInsert(List<?> list, boolean isIgnore) throws SQLException, NoSuchFieldException, IllegalAccessException;
        /**
         * 指定表名批量插入。只要用于modle字段相同，表名不同的情况下,如历史表。
         * @param list
         * @param tableName
         */
    public void bacthInsertSpecifiedTableName(List<?> list, String tableName) throws NoSuchFieldException, IllegalAccessException, SQLException;

}
