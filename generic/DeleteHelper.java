package com.msxf.psp.dal.generic;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by xi.chen on 2016/9/19 18:34.
 */
public interface DeleteHelper {
    /**
     * 通过list中的主键id删除数据
     * @param list
     * @return
     */
    int[] delete(List<?> list) throws SQLException, IllegalAccessException;

    /**
     * 通过sql删除数据
     * @param sql
     * @param param
     * @throws SQLException
     */
    void delete(String sql,Object... param) throws SQLException;
}
