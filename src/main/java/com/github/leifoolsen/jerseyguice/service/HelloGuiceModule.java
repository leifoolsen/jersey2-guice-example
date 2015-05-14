package com.github.leifoolsen.jerseyguice.service;

import com.google.inject.Binder;
import com.google.inject.Module;

import javax.inject.Singleton;

//public class HelloGuiceModule extends AbstractModule {
public class HelloGuiceModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(HelloService.class);
    }
}
