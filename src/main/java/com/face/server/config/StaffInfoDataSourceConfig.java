package com.face.server.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.face.server.mapper.staffInfo", sqlSessionFactoryRef = "staffInfoSqlSessionFactory")
public class StaffInfoDataSourceConfig {
    @Bean(name = "staffInfoDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public HikariDataSource getSqlserverDateSource() {
        return new HikariDataSource();
    }

    @Bean(name = "staffInfoSqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlserverSqlSessionFactory(@Qualifier("staffInfoDataSource") DataSource datasource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
        //mybatis扫描xml所在位置
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/staffInfo/*.xml"));
        return bean.getObject();
    }

    @Bean("staffInfoSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlserverSqlSessionTemplate(@Qualifier("staffInfoSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
