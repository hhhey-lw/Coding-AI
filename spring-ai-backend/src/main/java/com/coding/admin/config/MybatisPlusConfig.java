package com.coding.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置
 * @author weilong
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件
     * 使用dynamic-datasource时，分页插件会自动识别数据源类型
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加分页插件（会自动识别数据库类型）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setMaxLimit(500L); // 单页最大数量限制
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否进行处理
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
}

