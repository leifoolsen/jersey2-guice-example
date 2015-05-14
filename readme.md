#Jersey-2, Guice, Embedded Jetty
Maven project demonstrating how to run a JAX-RS 2 project in Embedded Jetty-9 with Servlet-3.1 annotation based 
configuration, using Jersey-2 with Guice injection and JSON binding via MOXy.

##Steps to run this project
* Fork, Clone or Download ZIP
* Build project: *mvn clean install -U*
* Start Jetty from project folder: *mvn exec:java*
* Application.wadl: *http://localhost:8080/api/application.wadl*
* Example usage: *http://localhost:8080/api/say/hello*
* Import project into your favourite IDE
* Open `HelloResourceTest.java` to start exploring code

###Note I
This project uses the [NetsOSS embedded-jetty bootstrapper](https://github.com/NetsOSS/embedded-jetty), 
see: `JettyMain.java` class. 

###Note II
You can package the project with the [appassembler-maven-plugin](http://mojo.codehaus.org/appassembler/appassembler-maven-plugin/)

* Build the project with the *appassembler* profile: *mvn install -Pappassembler* 
* ... then run the app from the project folder with the following command: <br/>sh _target/appassembler/bin/startapp_
* Open a browser and hit *http://localhost:8087/api/say/hello*
