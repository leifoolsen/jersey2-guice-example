package com.github.leifoolsen.jerseyguice.service;

import com.google.inject.Binder;
import com.google.inject.Module;

//public class HelloGuiceModule extends AbstractModule {
public class HelloGuiceModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(HelloService.class);
    }
}
