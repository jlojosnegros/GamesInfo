FROM maven as builder
RUN mkdir /project
COPY . /project
WORKDIR /project
RUN mvn clean package -B -DskipTests=true

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y default-jre
RUN mkdir /servidor
WORKDIR /servidor
COPY --from=builder /project/target/prueba_servidor-0.0.1-SNAPSHOT.jar /servidor
EXPOSE 8443
CMD java -jar /servidor/prueba_servidor-0.0.1-SNAPSHOT.jar --spring.datasource.url="jdbc:mysql://mysql:3306/gamesinfo_db?verifyServerCertificate=false&useSSL=true" --spring.datasource.username="root" --spring.datasource.password="gugus" --spring.jpa.hibernate.ddl-auto="update"