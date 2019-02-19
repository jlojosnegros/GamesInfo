FROM maven as builder
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -B -DskipTests=true
ENV app_jar_name = prueba_servidor-0.0.1-SNAPSHOT.jar
ENV application = /project/target/${app_jar_name}

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y default-jre
RUN mkdir /servidor
WORKDIR /servidor
COPY --from=builder ${application} /servidor
#CMD ["java","-jar","/servidor/${app_jar_name}"]
