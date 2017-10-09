@echo off
if not exist bin (
  mkdir bin
)
echo "Running javac"
"%JAVA_HOME%\bin\javac" -sourcepath src -d bin src/org/anichakra/tools/db/perfinder/Application.java
if errorlevel 1 (
  echo "Compilation failed!"
) else (

echo "Creating jar"
"%JAVA_HOME%\bin\jar" cvfe db-perfinder.jar  org.anichakra.tools.db.perfinder.Application -C bin/ .
if errorlevel 1 (
  echo "Jar creation failed!"
  
) else (
echo "Done."
)
)

