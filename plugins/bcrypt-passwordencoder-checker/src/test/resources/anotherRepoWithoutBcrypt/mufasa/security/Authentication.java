package com.mytaxi.prioritydriverservice.config.security;

import com.mytaxi.security.basicauth.ProtoBasicAuthentificationEntryPoint;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.ELRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class AuthenticationConfig extends GlobalAuthenticationConfigurerAdapter
{

    @Value("${api.user.username}")
    private String username;

    @Value("${api.user.password}")
    private String password;

    @Value("${spring.application.name}")
    private String applicationName;


    @Bean
    public static DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint(
        final ProtoBasicAuthentificationEntryPoint protoBasicAuthenticationEntryPoint,
        final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint)
    {
        final var matcher =
            new ELRequestMatcher(
                "hasHeader('Accept','application/json') or hasHeader('Accept','application/x-protobuf') or hasHeader('Content-Type','application/json') "
                    + "or hasHeader('Content-Type','application/x-protobuf')");

        final var map = new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>();
        map.put(matcher, protoBasicAuthenticationEntryPoint);

        final var delegatingAuthenticationEntryPoint = new DelegatingAuthenticationEntryPoint(map);
        delegatingAuthenticationEntryPoint.setDefaultEntryPoint(basicAuthenticationEntryPoint);

        return delegatingAuthenticationEntryPoint;
    }


    @Override
    public void init(final AuthenticationManagerBuilder auth) throws Exception
    {
        final var encoder = NoOpPasswordEncoder.getInstance(); //NOSONAR

        auth.inMemoryAuthentication()
            .passwordEncoder(encoder)
            .withUser(username)
            .password(password)
            .roles("API_USER");
    }


    @Bean
    public ProtoBasicAuthentificationEntryPoint protoBasicAuthenticationEntryPoint()
    {
        final var entryPoint = new ProtoBasicAuthentificationEntryPoint();
        entryPoint.setRealmName("mytaxi " + applicationName);
        return entryPoint;
    }


    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint()
    {
        final var entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("mytaxi " + applicationName);
        return entryPoint;
    }
}
