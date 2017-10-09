@echo off
if not exist log (
  mkdir log
)
"%JAVA_HOME%\bin\java" -Djdbc.properties=config/jdbc.properties -Djava.util.logging.config.file=config/logging.properties -jar db-perfinder.jar