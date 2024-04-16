package com.pj.client;

import cn.dev33.satoken.oauth2.model.SaClientModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientTemplateImpl {
    @Value("${superset.datasource.host}")
   private String host;

    @Value("${superset.datasource.port}")
   private int port;

    @Value("${superset.user.username}")
   private String username;

    @Value("${superset.user.password}")
   private String password;

    @Bean
    public Client getClient() {
        try {
            return new Client(host, port, username, password);
        } catch (Exception e) {
            log.error("++++++++++++++++++++++++++++++++Error creating client++++++++++++++++++++++++++++++++", e);
            throw new RuntimeException(e);
        }
    }

    public void refreshClient(Client client) {
        try {
            client.refreshClientByUsernameAndPassword(username, password);
        } catch (Exception e) {
            log.error("++++++++++++++++++++++++++++++++Error refreshing client++++++++++++++++++++++++++++++++", e);
            throw new RuntimeException(e);
        }
    }
}
