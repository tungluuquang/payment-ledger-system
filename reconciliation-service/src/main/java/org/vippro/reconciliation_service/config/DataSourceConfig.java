package org.vippro.reconciliation_service.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.account")
    DataSourceProperties accountDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.account.hikari")
    HikariDataSource accountDataSource(
            @Qualifier("accountDataSourceProperties")
            DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcClient accountJdbcClient(
            @Qualifier("accountDataSource") DataSource dataSource
    ) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    @ConfigurationProperties("spring.datasource.ledger")
    DataSourceProperties ledgerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.ledger.hikari")
    HikariDataSource ledgerDataSource(
            @Qualifier("ledgerDataSourceProperties")
            DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    JdbcClient ledgerJdbcClient(
            @Qualifier("ledgerDataSource") DataSource dataSource
    ) {
        return JdbcClient.create(dataSource);
    }
}
