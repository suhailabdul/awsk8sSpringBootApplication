FROM openjdk:8
COPY target/springbootecommerce.jar springbootecommerce.jar
EXPOSE 9080
RUN bash -c 'touch /springbootecommerce.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/springbootecommerce.jar"]