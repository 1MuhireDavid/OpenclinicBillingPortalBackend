package com.hospital.portal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    /**
     * Primary DataSource — openclinic DB.
     * Spring Boot auto-configures JPA EntityManagerFactory using this bean.
     * Defined via spring.datasource.* in application.yml.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource openclinicDataSource(
            @Qualifier("openclinicDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean("openclinicDataSourceProperties")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties openclinicDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Secondary DataSource — ocadmin DB (patient demographics).
     * Used only via JdbcTemplate; no JPA entities live here.
     */
    @Bean("ocadminDataSourceProperties")
    @ConfigurationProperties("app.datasource.ocadmin")
    public DataSourceProperties ocadminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("ocadminDataSource")
    public DataSource ocadminDataSource(
            @Qualifier("ocadminDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean("openclinicJdbcTemplate")
    @Primary
    public JdbcTemplate openclinicJdbcTemplate(DataSource openclinicDataSource) {
        return new JdbcTemplate(openclinicDataSource);
    }

    @Bean("ocadminJdbcTemplate")
    public JdbcTemplate ocadminJdbcTemplate(
            @Qualifier("ocadminDataSource") DataSource ocadminDataSource) {
        return new JdbcTemplate(ocadminDataSource);
    }
}
