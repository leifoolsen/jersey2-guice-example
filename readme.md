#Jersey-2, Guice, Embedded Jetty
Maven project demonstrating how to run a JAX-RS 2 project in Embedded Jetty-9 with Servlet-3.1 annotation based 
configuration, using Jersey-2 with Guice injection and JSON binding via MOXy.

##Steps to run this project
* Fork, Clone or Download ZIP
* Build project: *mvn clean install -U*
* Start Jetty from project folder: *mvn exec:java*
* ... or execute main method in `com.github.leifoolsen.jerseyguice.main.Main` from your IDE
* Application.wadl: *http://localhost:8080/api/application.wadl*
* Example usage: *http://localhost:8080/api/say/hello*
* Import project into your favourite IDE
* Open `HelloResourceTest.java` to start exploring code
