package me.jume.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;



public class GenEntityMysqlUtil {
	private static String[] fieldNames;		// 数据库字段名
	private static String[] colNames;		// JavaBean列名数组
	private static String[] colTypes;		// 列名类型数组
	private static int[] colSizes;			// 列名大小数组
	
	private static boolean f_util = false;		// 需要导入java.util.*;
	private static boolean f_sql = false;		// 需要导入java.sql.*;
	
	private static final String NAME_END = "VO";	// 实体类的命名规范，后缀
	private static String primaryKey = "";		// 主键
	
	static Connection conn = DBUtils.getCon();		// 获取数据库的连接
	
	/**
	 * 根据数据库中的表名tableName生成JavaBean写出到packagePath路径下
	 * @param packagePath
	 * @param tableName
	 */
	public static void genEntity(String packagePath,String tableName){
		PreparedStatement ps = null;
		String sql = "select * from " + tableName;
		try {
			// 获取元数据，包含了这张表的表结构信息
			DatabaseMetaData dbmd = conn.getMetaData();
			// 获取主键列（建议断点调试）
			ResultSet primaryKeys =dbmd.getPrimaryKeys(null, null, tableName);
			while (primaryKeys.next()) {
				primaryKey = primaryKeys.getString(4);
				System.out.println("主键名称：" + primaryKey);
			}
			primaryKeys.close();
			ps = conn.prepareStatement(sql);
			ResultSetMetaData rsmd = ps.getMetaData();
			int size = rsmd.getColumnCount();		// 获取总共有多少列
			fieldNames = new String[size];
			colNames = new String[size];
			colTypes = new String[size];
			colSizes = new int[size];
			for(int i=0; i<rsmd.getColumnCount(); i++){
				fieldNames[i] = rsmd.getColumnName(i+1);
				colNames[i] = ExTypeUtils.getCamelStr(fieldNames[i]);
				colTypes[i] = rsmd.getColumnTypeName(i+1);
				if (colTypes[i].equalsIgnoreCase("datetime")) {
					f_util = true;
				}
				if (colTypes[i].equalsIgnoreCase("image")||
						colTypes[i].equalsIgnoreCase("text")) {
					f_sql = true;
				}
				colSizes[i] = rsmd.getColumnDisplaySize(i+1);
			}
			// 对上述获取到的相关的数据库的字段和数据类型信息，进行解析，拼接得到JavaBean的字符串
			String content = parse(colNames,colTypes,colSizes,packagePath,tableName);
			System.out.println(content);
			String path = System.getProperty("user.dir") + "/src/" +
					packagePath.replaceAll("\\.", "/");
			File dir = new File(path);		// 在当前项目相应位置创建包，用于存放生成的JavaBean
			if (!dir.exists()) {
				dir.mkdirs();
			}
			// 在此文件夹中创建一个Java文件，并且将内存总共的JavaBean字符串写出到文件中
			String resPath = path + "/" + ExTypeUtils.initcap(tableName)+ NAME_END + ".java";
			FileUtils.writeStr2File(new File(resPath), content);
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e2) {
			}
		}
	}

	/**
	 * 对上述获取到的相关的数据库的字段和数据类型信息，进行解析，拼接得到JavaBean的字符串
	 * @param colNames2
	 * @param colTypes2
	 * @param colSizes2
	 * @param packagePath
	 * @param tableName
	 * @return
	 */
	private static String parse(String[] colNames2, String[] colTypes2, int[] colSizes2, String packagePath,
			String tableName) {
		StringBuffer sb = new StringBuffer();
		// ① package 语句		由用户指定放到项目中哪个报下
		sb.append("package " + packagePath + ";\r\n\r\n");
		// ② import 语句			序列化、注解、时间日期类
		sb.append("import java.io.Serializable;\r\n");
		if (f_util) {
			sb.append("import java.util.Date;\r\n");
		}
		if (f_sql) {
			sb.append("import java.sql.*;\r\n");
		}
		sb.append("import javax.persistence.Column;\r\n");
		sb.append("import javax.persistence.Entity;\r\n");
		sb.append("import javax.persistence.Id;\r\n");
		sb.append("import javax.persistence.Table;\r\n");
		// ③ 类名及其类上的注解
		sb.append("@Entity\r\n");
		sb.append("@Table(name=\"").append(tableName).append("\")\r\n");
		sb.append("public class ").append(ExTypeUtils.initcap(tableName));
		sb.append(NAME_END).append(" implements Serializable {\r\n\r\n");
		
		// ④ 类内部的成员变量，getXXX、setXXX
		processAllAttrs(sb);		// 生成所有属性并追加到sb上
		sb.append("\r\n");
		processAllMethods(sb);
		sb.append("}\r\n");
		return sb.toString();
	}

	// 生成所有get、set方法并追加到sb上
	private static void processAllMethods(StringBuffer sb) {
		for (int i = 0; i < colNames.length; i++) {
			// 添加注解
			if(primaryKey.equals(fieldNames[i])){
				sb.append("\t@Id").append("\r\n");
			}
			sb.append("\t@Column(name = \"").append(fieldNames[i]).append("\")").append("\r\n");
			
			sb.append("\tpublic " + ExTypeUtils.toJavaType(colTypes[i]) + " get"
					+ ExTypeUtils.initcap(colNames[i]) + "(){\r\n");
			sb.append("\t\treturn " + colNames[i] + ";\r\n");
			sb.append("\t}\r\n");
			
			sb.append("\tpublic void set" + ExTypeUtils.initcap(colNames[i]) + "("
					+ ExTypeUtils.toJavaType(colTypes[i]) + " " + colNames[i]
					+ "){\r\n");
			sb.append("\t\tthis." + colNames[i] + " = " + colNames[i] + ";\r\n");
			sb.append("\t}\r\n\r\n");
		}
	}

	// 生成所有属性并追加到sb上
	private static void processAllAttrs(StringBuffer sb) {
		for(int i=0; i<colNames.length; i++){
			sb.append("\tprivate ")
			.append(ExTypeUtils.toJavaType(colTypes[i]))
			.append(" " + colNames[i] + ";\r\n");
		}
	}
}
