package org.anichakra.tools.db.perfinder.rdbms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;

public class RdbmsPerfinder implements AutoCloseable {

    Connection connection = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    DriverProxy shim = null;

    static class TypeObject {
        Class<?> type;
        Class<?> clazz;
        String name;

        Function<String, ?> function;

        public TypeObject(String name, Class<?> type, Function<String, ?> function) {
            this.type = type;
            this.name = name;
            this.function = function;
        }
    }

    static Map<String, TypeObject> typeMap = new HashMap<>();
    static {
        typeMap.put("int", new TypeObject("Int", Integer.TYPE, c -> Integer.valueOf(c)));
        typeMap.put("long", new TypeObject("Long", Long.TYPE, c -> Long.valueOf(c)));
        typeMap.put("double", new TypeObject("Double", Double.TYPE, c -> Double.valueOf(c)));
        typeMap.put("float", new TypeObject("Float", Float.TYPE, c -> Float.valueOf(c)));
        typeMap.put("bool", new TypeObject("Boolean", Boolean.TYPE, c -> Boolean.valueOf(c)));
        typeMap.put("short", new TypeObject("Short", Short.TYPE, c -> Short.valueOf(c)));
        typeMap.put("string", new TypeObject("String", String.class, c -> new String(c)));
    }

    static class DriverProxy implements Driver {
        private Driver driver;

        DriverProxy(Driver d) {
            this.driver = d;
        }

        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override

        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public void loadDriver(String jdbcDriverClassName, String jarPath) {
        if (jdbcDriverClassName == null || jdbcDriverClassName.trim().length() == 0)
            throw new IllegalArgumentException("Driver class name not mentioned in jdbc.driver!");

        try {
            if (jarPath != null) {
                URL u = new URL("jar:file:" + jarPath.trim() + "!/");
                URLClassLoader ucl = new URLClassLoader(new URL[] { u });
                Driver d = (Driver) Class.forName(jdbcDriverClassName.trim(), true, ucl).newInstance();
                shim = new DriverProxy(d);
                DriverManager.registerDriver(shim);
            } else {
                Class.forName(jdbcDriverClassName);
            }

        } catch (ClassNotFoundException | MalformedURLException | InstantiationException | IllegalAccessException
                | SQLException e) {

            throw new RuntimeException("Driver Cannot be loaded!", e);

        }
    }

    public void createConnection(String connectionUrl, String username, String password) {
        if (connectionUrl == null || connectionUrl.trim().length() == 0)
            throw new IllegalArgumentException("ConnectionUrl name not mentioned in jdbc.driver!");
        try {
            if (username == null || username.trim().length() == 0)
                connection = DriverManager.getConnection(connectionUrl.trim());
            else
                connection = DriverManager.getConnection(connectionUrl.trim(), username.trim(), password);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create Connection!", e);
        }
    }

    public void prepareStatement(String query, Integer fetchSize, Integer maxRows, String... parameters) {
        if (query == null || query.trim().length() == 0)
            throw new IllegalArgumentException("query is not provided!");
        assert connection != null : "Connection is null! Load JDBC Driver and create Connection First";
        try {
            pstmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            if (parameters != null) {
                setParameters(parameters, pstmt);
            }
            if (fetchSize != null)
                pstmt.setFetchSize(fetchSize);
            if(maxRows!=null)
                pstmt.setMaxRows(maxRows);
        } catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create PreparedStatement!", e);
        }
    }

    public void executeQuery() {
        assert pstmt != null : "Prepare a Statement first!";
        try {
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot execute query!", e);

        }
    }

    public List<Map<String, Object>> fetchResult(Integer rowIndex) {

        List<Map<String, Object>> result = new LinkedList<>();
        try {
            String[] columns = getColumns(rs);
            if (rowIndex != null)
                rs.absolute(rowIndex);

         //   int count = 0;
            while (rs.next()) {
              //  if (maxRows != null && count >= maxRows)
                 //   break;
                Map<String, Object> row = mapRow(rs, columns);
                result.add(row);
              //  count++;
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot fetch result!", e);
        }
        return result;
    }

    private static Map<String, Object> mapRow(ResultSet rs, String[] columns) throws SQLException {
        Map<String, Object> record = new LinkedHashMap<>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            record.put(columns[i], rs.getObject(i + 1));
        }
        return record;
    }

    private static String[] getColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        String[] columns = new String[rsmd.getColumnCount()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = rsmd.getColumnName(i + 1);
        }
        return columns;
    }

    private static void setParameters(String[] parameters, PreparedStatement pstmt)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (int i = 0; i < parameters.length; i++) {
            String type = "string";
            if (parameters[i].contains(":")) {
                String[] tokens = parameters[i].split(":");
                type = tokens[1];
                parameters[i] = tokens[0];
            }

            Method method = PreparedStatement.class.getMethod("set" + typeMap.get(type).name, Integer.TYPE,
                    typeMap.get(type).type);
            method.invoke(pstmt, i + 1, typeMap.get(type).function.apply(parameters[i]));
        }
    }

    @Override
    public void close() throws Exception {
        if (pstmt != null && !pstmt.isClosed()) {
            try {
                pstmt.close();
            } catch (SQLException e) {
            }
        }
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
        if (shim != null) {
            DriverManager.deregisterDriver(shim);
        }

    }

}
