 package com.bitmoi.order.router;

 import dev.miku.r2dbc.mysql.MySqlConnectionConfiguration;
 import dev.miku.r2dbc.mysql.MySqlConnectionFactory;
 import io.r2dbc.spi.ConnectionFactory;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.core.io.ClassPathResource;
 import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
 import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
 import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
 import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
 import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
 import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;


 @Configuration
 @EnableR2dbcRepositories
 @EnableR2dbcAuditing
 public class DataSourceR2DBCConfig extends AbstractR2dbcConfiguration {

     @Override
     public ConnectionFactory connectionFactory() {
         return MySqlConnectionFactory.from(
                 MySqlConnectionConfiguration.builder().build()
         );
     }

     @Bean
     public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
         ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
         initializer.setConnectionFactory(connectionFactory);
         CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
         initializer.setDatabasePopulator(populator);
         return initializer;
     }

 }
