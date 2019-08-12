package com.jbr.middletier;

import com.jbr.middletier.backup.manager.BackupManager;
import com.jbr.middletier.backup.type.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Created by jason on 08/02/17.
 */

@Configuration
@ComponentScan("com.jbr.middletier")
public class BackupConfig {
    @Value("${middle.tier.backup.db.url}")
    private String url;

    @Value("${middle.tier.backup.db.username}")
    private String username;

    @Value("${middle.tier.backup.db.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
        dataSourceBuilder.url(url);
        dataSourceBuilder.username(username);
        dataSourceBuilder.password(password);

        return dataSourceBuilder.build();
    }
}
