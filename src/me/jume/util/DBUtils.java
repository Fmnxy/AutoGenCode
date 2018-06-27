package me.jume.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

// 数据库连接管理类，获取数据库连接
public class DBUtils {
	private static String driver;
	private static String url;
	private static String user;
	private static String pwd;
	private static Connection con;
	static{
		ResourceBundle rsb = ResourceBundle.getBundle("db");
		driver = rsb.getString("driver");
		url = rsb.getString("url");
		user = rsb.getString("user");
		pwd = rsb.getString("pwd");
	}
	public static Connection getCon(){
		try {
			Class.forName(driver);
			// 【2】获取之前，先判断是否已经有对应的实例，如果没有重新获取一次
			if(con == null){
				con = DriverManager.getConnection(url, user, pwd);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return con;
	}
	public static void main(String[] args) {
		System.out.println(DBUtils.getCon());
	}
	public static void closeAll(ResultSet rs, PreparedStatement ps, Connection con){
		try{
			if(rs != null ) rs.close(); 
			if(ps != null ) ps.close(); 
			if(con != null ) con.close(); 
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
