FROM java:alpine
ADD build/libs/xenon-talk-standalone.jar . 
ENTRYPOINT ["java","-jar","xenon-talk-standalone.jar"]
