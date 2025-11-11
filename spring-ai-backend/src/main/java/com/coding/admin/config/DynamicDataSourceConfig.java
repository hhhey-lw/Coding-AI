package com.coding.admin.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 动态数据源配置
 * @author weilong
 */
@Configuration
public class DynamicDataSourceConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * MySQL JdbcTemplate（默认数据源）
     */
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        return new JdbcTemplate(ds.getDataSource("mysql"));
    }

    /**
     * PostgreSQL JdbcTemplate（用于PgVector）
     */
    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate() {
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        return new JdbcTemplate(ds.getDataSource("postgresql"));
    }
}

