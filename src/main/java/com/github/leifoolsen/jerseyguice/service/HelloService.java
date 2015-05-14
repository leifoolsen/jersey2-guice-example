package com.github.leifoolsen.jerseyguice.service;

import com.github.leifoolsen.jerseyguice.domain.HelloBean;

import javax.inject.Singleton;

@Singleton
public class HelloService {
    public HelloBean sayHello() {
        return new HelloBean("Hello from Guice injected service!");
    }
}
