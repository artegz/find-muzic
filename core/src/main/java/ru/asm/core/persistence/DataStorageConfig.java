package ru.asm.core.persistence;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Driver;

/**
 * User: artem.smirnov
 * Date: 15.06.2016
 * Time: 9:52
 */
@Configuration
@EnableTransactionManagement
public class DataStorageConfig {

    private static final String INIT_SCHEMA_LOCATION = "schema/schema.sql";
    private static final String DB_URL = "jdbc:h2:file:C:\\TEMP\\find-music\\h2\\fmdb";

    @Bean
    public DataSource getDataSource() {
        final DataSource dataSource;
        try {
            //noinspection unchecked
            final Class<Driver> klazz = (Class<Driver>) Class.forName("org.h2.Driver");
            final Driver driver = klazz.newInstance();

            dataSource = new SimpleDriverDataSource(driver, DB_URL);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
        return dataSource;
    }

    @Bean
    @Autowired
    public PlatformTransactionManager getPlatformTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "sqlSessionFactory")
    @Autowired
    public SqlSessionFactoryBean getSqlSessionFactoryBean(DataSource dataSource) {
        final SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean;
    }

    @Bean
    public MapperScannerConfigurer getMapperScannerConfigurer() {
        final MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        mapperScannerConfigurer.setBasePackage("ru.asm.core.persistence.mappers");
        return mapperScannerConfigurer;
    }

    @Bean
    @Autowired
    public DataSourceInitializer getDataSourceInitializer(DataSource dataSource) {
        final DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        final ClassPathResource schema = new ClassPathResource(INIT_SCHEMA_LOCATION, getClass());
        dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(true, true, null, schema));
        dataSourceInitializer.setDataSource(dataSource);
        return dataSourceInitializer;
    }

    @Bean
    public DataStorageService getDataStorageService() {
        return new DataStorageService();
    }
}
