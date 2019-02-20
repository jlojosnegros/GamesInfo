FROM maven as builder
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -B -DskipTests=true

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y default-jre
RUN mkdir /mailService
WORKDIR /mailService
COPY --from=builder /project/target/mailService-0.0.1-SNAPSHOT.jar /mailService
EXPOSE 8080
CMD java -jar /mailService/mailService-0.0.1-SNAPSHOT.jar