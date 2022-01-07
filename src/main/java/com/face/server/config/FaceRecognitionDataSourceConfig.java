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
@MapperScan(basePackages = "com.face.server.mapper.faceRecognition", sqlSessionFactoryRef = "faceRecognitionSqlSessionFactory")
public class FaceRecognitionDataSourceConfig {
    @Bean(name = "faceRecognitionDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public HikariDataSource getSqlserverDateSource() {
        return new HikariDataSource();
    }

    @Bean(name = "faceRecognitionSqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlserverSqlSessionFactory(@Qualifier("faceRecognitionDataSource") DataSource datasource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(datasource);
        //mybatis扫描xml所在位置
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/faceRecognition/*.xml"));
        return bean.getObject();
    }

    @Bean("faceRecognitionSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlserverSqlSessionTemplate(@Qualifier("faceRecognitionSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
