package uk.gov.hmcts.ccd.definition.store.repository.am.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@PropertySource({"classpath:application.properties"})
public class AmDataSourceConfiguration {

    @Bean(name = "definitionStoreTransactionManager")
    @Autowired
    public PlatformTransactionManager tm1(@Qualifier("definitionDataSource") DataSource datasource) {
        return new DataSourceTransactionManager(datasource);
    }

    @Bean(name = "amDatasourceTransactionManager")
    @Autowired
    public PlatformTransactionManager tm2(@Qualifier("amDataSource") DataSource datasource) {
        return new DataSourceTransactionManager(datasource);
    }

    @Bean(name = "transactionManager")
    public ChainedTransactionManager transactionManager(@Qualifier("definitionStoreTransactionManager") PlatformTransactionManager ds1,
                                                        @Qualifier("amDatasourceTransactionManager") PlatformTransactionManager ds2) {
        return new ChainedTransactionManager(ds1, ds2);
    }

    @Primary
    @Bean(name = "definitionDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("amDataSource")
    @ConfigurationProperties(prefix = "am.datasource")
    public DataSource getAmDataSource() {
        return DataSourceBuilder.create().build();
    }
}