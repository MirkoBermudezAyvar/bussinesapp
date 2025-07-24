FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.6_10
RUN apk add --no-cache tzdata
ENV TZ='America/Lima'
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

COPY target/*.jar application.jar

ADD newrelic /app
ADD newrelic /app

CMD ["java", "-javaagent:/app/newrelic.jar","-jar","application.jar"]