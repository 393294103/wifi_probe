package utils;
import conf.ConfigurationManager;
import constants.Constants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;


public class JDBCHelper {
    static {

        String driver = ConfigurationManager.getProperty(Constants.JDBC_DRIVER);
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static JDBCHelper instanse = null;
    public static JDBCHelper getInstanse(){
        if (instanse == null){
            synchronized (JDBCHelper.class){
                if (instanse == null){
                    instanse = new JDBCHelper();
                }
            }

        }
        return instanse;
    }

    //数据库连接池
    private LinkedList<Connection> datasource = new LinkedList<Connection>();

    private JDBCHelper() {
        int datasourceSize = ConfigurationManager.getInteger(Constants.DBC_DATASOURCE_SIZE);
        //创建指定连接数量的数据库连接池
        for (int i = 0; i < datasourceSize; i++) {
            boolean local = ConfigurationManager.getBoolean(Constants.MYSQL_LOCAL);
            String url = null;
            String user = null;
            String password = null;

            if (local) {
                url = ConfigurationManager.getProperty(Constants.JDBC_URL);
                user = ConfigurationManager.getProperty(Constants.JDBC_USER);
                password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD);
            } else {
                url = ConfigurationManager.getProperty(Constants.JDBC_URL_PROD);
                user = ConfigurationManager.getProperty(Constants.JDBC_USER_PROD);
                password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD_PROD);
            }

            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                datasource.push(conn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized Connection getConnection() {
        while (datasource.size() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return datasource.poll();
    }


    /**
     * 执行查询SQL语句
     * @param sql
     * @param params
     * @param callback
     */
    public void executeQuery(String sql,Object[] params,QueryCallback callback){
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);

            if(params != null && params.length > 0) {
                for(int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }

            rs = pstmt.executeQuery();

            callback.process(rs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(conn != null) {
                datasource.push(conn);
            }
        }
    }

    /**
     * 执行SQL语句
     * @param sql
     * @param params
     */
    public void execute(String sql,Object[] params){
        Connection conn = null;
        PreparedStatement pstmt = null;
        Boolean rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);

            if(params != null && params.length > 0) {
                for(int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }

            rs = pstmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(conn != null) {
                datasource.push(conn);
            }
        }
    }

    /**
     * 静态内部类：查询回调接口
     * @author Administrator
     *
     */
    public static interface QueryCallback{

        /**
         * 处理查询结果
         * @param rs
         * @throws Exception
         */
        void process(ResultSet rs) throws Exception;

    }

}
