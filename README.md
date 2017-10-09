# db-perfinder

A small application or tool built in pure Java to find performance of any query when run in any RDBMS database.

## Getting Started

To use this tool you need to install JDK 1.8 in your PC or Server. The tool requires JDBC configuration to be set up in a properties file to connect to any RDBMS databases like Oracle, MySQL, Postgres, Sybase, DB2, SqlServer etc. A sample configuration is given in ./config folder. It uses the type-4 JDBC driver, hence the corresponding jar need to be placed in a path and need to be mentioned in the configuration, so that it can be loaded dynamically and externally outside classpath. 

The tool executes a query (can be parameterized also) in 3 times by default and provides the average query execution time and the data fetch time. The number of query executions as part of the test can be configurable. The tool also reports the records as a result of the query along with number of records fetched. One can provide pagination to the query also v.i.z. begin index or row index and number of rows to fetch.

### Prerequisites
Only JDK 1.8 need to be installed and connectivity to the database where the query will be run. You need to set or JAVA_HOME that points to correct JDK installation directory. Optionally you can install Git to clone this source code or you can simply download it and keep it in any prefered directory.

Check JAVA_HOME:

```
echo $JAVA_HOME
```
if not found then add it to your profile (UNIX) or environment variable (Windows). Also can set or export like below:

```
set JAVA_HOME=/opt/jdk1.8_144
```

### Installing

Git clone to get the source code from github:

```
git clone https://github.com/anichakra/db-perfinder
```

You will find 'db-perfinder' directory created under $HOME/git path. Go inside 'db-perfinder' and run 'compile' to compile and create the db-perfinder.jar file.

```
cd db-perfinder
./compile
```

## Running the application

After the db-perfinder.jar is succesfully created, update the JDBC properties in config/jdbc.properties file. Please check the documentation as provided in the sample properties file. Make sure the JDBC type-4 jar pertaining to the database should be placed in a preferred path and that is configured in jdbc.properties. It recommended to create a lib directory inside 'db-perfinder' and keep all your DB driver jars there.

To run the application just fire the run command.

```
./run
```

In case of Windows use the .bat files instead.

### Logging

The application uses java.util.logging to log INFO, FINE and SEVERE logs. By default the console logs INFO and SEVERE while FINE logs are printed in the log file created in 'log' directory. The logging configuration is placed in config/logging.properties. This can be changed accordingly. A sample result in log file is given below:

```
JDBC Properties:{jdbc.jarPath=/home/db-perfinder/lib/postgresql-42.1.4.jar, jdbc.maxRows=100, jdbc.queryRun=9, jdbc.url=jdbc:postgresql://13.126.121.227:5432/fleet, jdbc.query=select mk.code as make_code, mk.title as make_title, md.code, md.title from public.model md, public.make mk where mk.id=md.make_id and mk.code=?, jdbc.parameters=ACURA, jdbc.driver=org.postgresql.Driver  , jdbc.username=postgres , jdbc.rowIndex=10, jdbc.fetchSize=10}
Loading JDBC Driver: org.postgresql.Driver  
PostgreSQL JDBC Driver 42.1.4
  setDefaultFetchSize = 0
  setPrepareThreshold = 5
Receive Buffer Size is 187,200
Send Buffer Size is 43,520
Executing Query: select mk.code as make_code, mk.title as make_title, md.code, md.title from public.model md, public.make mk where mk.id=md.make_id and mk.code=?
Query Executed Successfully!
Record Count: 14
Output: 

---------------------------------------------------
|make_code|make_title|code     |title             |
---------------------------------------------------
|ACURA    |Acura     |RDX      |RDX               |
|ACURA    |Acura     |RL_MODELS|RL Models (2)     |
|ACURA    |Acura     |3.5RL    | - 3.5 RL         |
|ACURA    |Acura     |RL       | - RL             |
|ACURA    |Acura     |RSX      |RSX               |
|ACURA    |Acura     |SLX      |SLX               |
|ACURA    |Acura     |TL_MODELS|TL Models (3)     |
|ACURA    |Acura     |2.5TL    | - 2.5TL          |
|ACURA    |Acura     |3.2TL    | - 3.2TL          |
|ACURA    |Acura     |TL       | - TL             |
|ACURA    |Acura     |TSX      |TSX               |
|ACURA    |Acura     |VIGOR    |Vigor             |
|ACURA    |Acura     |ZDX      |ZDX               |
|ACURA    |Acura     |ACUOTH   |Other Acura Models|
---------------------------------------------------

Executing Tests...
Executing Test Run: 1
Executing Test Run: 2
Executing Test Run: 3
Executing Test Run: 4
Executing Test Run: 5
Executing Test Run: 6
Executing Test Run: 7
Executing Test Run: 8
Executing Test Run: 9
Test Result...
Query Times (ms): All[72, 72, 73, 71, 71, 71, 71, 71, 72], Avg[72], Stdiv[0.7]
Fetch Times (ms): All[2, 1, 1, 1, 1, 1, 0, 0, 1], Avg[1], Stdiv[0.6]
Closing Connection and Exiting Gracefully!

```

## Deployment

Add additional notes about how to deploy this on a live system


## Authors

* **Anirban Chakraborty** - *Initial work* - [Anichakra](https://github.com/anichakra)

See also the list of [contributors](https://github.com/anichakra/db-perfinder/contributors) who participated in this project.





