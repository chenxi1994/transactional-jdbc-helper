# transactional-jdbc-helper
基于spring事务的jdbc工具类，能自动提交事务，会根据方法事务将数据源链接绑定到线程上面，保证一个方法事务获取的是同一个链接。
