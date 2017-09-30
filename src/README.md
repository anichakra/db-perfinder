mkdir bin

javac -sourcepath src -d bin src/org/anichakra/tools/db/perfinder/Application.java


jar cvfe db-perfinder.jar  org.anichakra.tools.db.perfinder.Application -C bin/ .


java -Djdbc.driver=org.postgresql.Driver -Djdbc.url=jdbc:postgresql://13.126.121.227:5432/fleet -Djdbc.username=postgres  -Djdbc.fetchSize=10 -Djdbc.query="select mk.code as make_code, mk.title as make_title, md.code, md.title from public.model md, public.make mk where mk.id=md.make_id and mk.code=? " -Djdbc.rowIndex=5 -Djdbc.maxRows=10  -Djdbc.parameters=ACURA -Djdbc.jarPath=/home/anirban/workspace/solnarchprog/db-perfinder/lib/postgresql-42.1.4.jar -jar db-perfinder.jar