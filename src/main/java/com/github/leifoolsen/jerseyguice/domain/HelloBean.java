package com.github.leifoolsen.jerseyguice.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HelloBean {
    private String say;

    protected HelloBean() {}
    public HelloBean(final String say) { this.say = say; }
    public String say() { return say; }
}
