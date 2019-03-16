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
RUN sudo apt-get install -y mysql-client
RUN mkdir /mailService
WORKDIR /mailService
COPY --from=builder /project/target/mailService-0.0.1-SNAPSHOT.jar /mailService
EXPOSE 8080
CMD java -jar /mailService/mailService-0.0.1-SNAPSHOT.jar
