# spring-boot-microservices

## Build mit Maven Wrapper

Bitte einmal testen, ob mit
```
cd aufgabe_1
./mvnw clean install
```
die Applikation kompiliert werden kann.

Erwarteter Output:
```
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.073 s -- in hotel.service.HotelTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] 
[INFO] --- jar:3.4.2:jar (default-jar) @ hotel-booking-kata ---
[INFO] Building jar: **/spring-boot-microservices/aufgabe_1/target/hotel-booking-kata-1.0-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot:3.5.4:repackage (repackage) @ hotel-booking-kata ---
[INFO] Replacing main artifact **/spring-boot-microservices/aufgabe_1/target/hotel-booking-kata-1.0-SNAPSHOT.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] The original artifact has been renamed to **/spring-boot-microservices/aufgabe_1/target/hotel-booking-kata-1.0-SNAPSHOT.jar.original
[INFO] 
[INFO] --- failsafe:3.1.2:integration-test (default) @ hotel-booking-kata ---
[INFO] 
[INFO] --- failsafe:3.1.2:verify (default) @ hotel-booking-kata ---
[INFO] 
[INFO] --- install:3.1.4:install (default-install) @ hotel-booking-kata ---
[INFO] Installing ***/spring-boot-microservices/aufgabe_1/pom.xml to **/.m2/repository/org/example/hotel-booking-kata/1.0-SNAPSHOT/hotel-booking-kata-1.0-SNAPSHOT.pom
[INFO] Installing **/spring-boot-microservices/aufgabe_1/target/hotel-booking-kata-1.0-SNAPSHOT.jar to **/.m2/repository/org/example/hotel-booking-kata/1.0-SNAPSHOT/hotel-booking-kata-1.0-SNAPSHOT.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.448 s
[INFO] Finished at: 2025-10-27T06:54:44+01:00
[INFO] ------------------------------------------------------------------------
```

## Applikationsstart mit Maven Wrapper

Start der Applikation via
```
cd aufgabe_1
./mvnw spring-boot:run
```

U.a. sollte dann die h2 Konsole unter http://localhost:8080/h2-console/ erreichbar sein.

## docker-compose nutzen

```
docker-compose up -d --build
```

sollte den Hotel-Service sowie einen Zipkin Server starten.

Mit 

```
docker-compose ps
```

werden die laufenden Container aufgelistet:

```
NAME                    IMAGE                                             COMMAND               SERVICE                 CREATED         STATUS                     PORTS
hotel-booking-service   spring-boot-microservices-hotel-booking-service   "java -jar app.jar"   hotel-booking-service   2 minutes ago   Up 2 minutes (unhealthy)   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
zipkin                  openzipkin/zipkin:latest                          "start-zipkin"        zipkin                  2 minutes ago   Up 2 minutes (healthy)     0.0.0.0:9411->9411/tcp, [::]:9411->9411/tcp

```

Der Zipkin Server ist unter http://localhost:9411/ erreichbar.


Anschließend die Services wieder mit
```
docker-compose down
```
herunterfahren.

# Referenzen

## Spring REST Client

:bangbang: Hinweis: RestTemplate und Spring Cloud OpenFeign werden nicht mehr weiterentwickelt. Es wird empfohlen, stattdessen [Spring REST Client](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface) zu verwenden.

## Kopplung und Kohäsion

[A Daily Practice of Empirical Software Design - Kent Beck - DDD Europe 2023](https://youtu.be/yBEcq23OgB4?si=PAEP7DgKhxvLJvTJ) - Vortrag von Kent Beck auf der DDD Europe 2023

## Architekturdokumentation

[Bessere Architekturdoku mit Hilfsmitteln aus dem Domain-driven Design – INNOQ Technology Lunch](https://www.youtube.com/live/g-4lCDsW1FE?si=JvbkNPbNZE3Vtmt8) - Vortrag von Michael Plöd

[Arc42](https://arc42.de/)

[C4 Model](https://c4model.com/) - von Simon Brown

## Event Storming

[Event Storming](https://eventstorming.com/) von Alberto Brandolini

[Event Storming Glossary Sheet](https://github.com/ddd-crew/eventstorming-glossary-cheat-sheet) - Anleitung zum Event Storming

## Collaborative Modeling

[DDD Starter Modling Process](https://github.com/ddd-crew/ddd-starter-modelling-process) - Übersicht über Collaborative Modeling Mehtoden

[CoMo Camp](https://comocamp.org) - Unconference zu Collaborative Modeling Methoden

## Mutation Testing in Java

[Mutation Testing](https://pitest.org)

## SBOM - Software Bill of Materials

Beispiele für Tools:

[CycloneDX Maven Plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin)

[OWASP Dependency Track](https://dependencytrack.org)

## Bücher

[Langlebige Softwarearchitekturen](https://dpunkt.de/produkt/langlebige-software-architekturen-4/) von Carola Lilienthal

[Get Your Hands Dirty on Clean Architecture](https://leanpub.com/get-your-hands-dirty-on-clean-architecture) von Tom Hombergs