FROM java:8
COPY ./proxyserver/target/proxyserver-*-jar-with-dependencies.jar /var/www/java/proxyserver.jar
WORKDIR /var/www/java
EXPOSE 9090
CMD ["java", "-jar","proxyserver.jar"]