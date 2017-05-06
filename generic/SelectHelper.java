package com.msxf.psp.dal.generic;

import java.util.List;

/**
 * 查询帮助类
 * Created by xi.chen on 2016/9/19 16:17.
 */
public interface SelectHelper {
    <T> List<T> select(Class c, String sql, Object... params);

    //List<Map<String, Object>> select(String sql, Object... params);
}
