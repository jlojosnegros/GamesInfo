FROM maven:3.6.0-jdk-8-alpine as builder
RUN mkdir /project
COPY ./pom.xml /project/pom.xml
WORKDIR /project
RUN mvn dependency:go-offline
COPY src/ /project/src/
RUN mvn package

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y software-properties-common wget unzip && sudo add-apt-repository ppa:openjdk-r/ppa && sudo apt-get update
RUN sudo apt-get install -y openjdk-8-jre
RUN mkdir /servidor
WORKDIR /servidor
COPY --from=builder /project/target/prueba_servidor-0.0.1-SNAPSHOT.jar /servidor
EXPOSE 8080
CMD java -jar /servidor/prueba_servidor-0.0.1-SNAPSHOT.jar --spring.datasource.url="jdbc:mysql://jlojosnegros-mysql-svc:3306/gamesinfo_db?verifyServerCertificate=false&useSSL=true" --spring.datasource.username="root" --spring.datasource.password="gugus" --spring.jpa.hibernate.ddl-auto="update"