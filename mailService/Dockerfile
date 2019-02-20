FROM maven as builder
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -B -DskipTests=true

ENV app_jar_name = mailService-0.0.1-SNAPSHOT.jar
ENV application = /project/target/${app_jar_name}
ENV destination_folder = /mailService

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y default-jre
RUN mkdir ${destination_folder}
WORKDIR ${destination_folder}
COPY --from=builder ${application} ${destination_folder}
#CMD ["java","-jar","/mailService/${app_jar_name}"]
CMD java -jar ${destination_folder}/${app_jar_name}