package actionsBD;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.text.DecimalFormat;

public class MySQLToText {
	// extracts the mysql table into text file at format att1;&;att2;&;...;&;attn;&;
	public static void mySQLToText(String table,String fileName) throws IOException{
		PrintWriter ecrivain=null;
		
		MySQLConnection cli=new MySQLConnection();
		ResultSet rs;
		try{
			File f=new File(fileName);
			ecrivain=new PrintWriter(new BufferedWriter(new FileWriter(f)));
			DecimalFormat format = new DecimalFormat();
			format.setMaximumFractionDigits(4);
			
			rs=cli.exec("select * from "+table);
			if (rs!=null){
				String s="";
				ResultSetMetaData rsmd = rs.getMetaData();
				int nbCols=rsmd.getColumnCount();
				for(int i=1;i<=nbCols;i++){
					s+=rsmd.getColumnName(i)+";&;";
				}
				ecrivain.println(s);
				
				while(rs.next()){
					s="";
					for(int i=1;i<=nbCols;i++){
						s+=rs.getString(i)+";&;";
					}
					ecrivain.println(s);
				}
			}
		}
		catch(SQLException e){
			System.out.println("Exception "+e);
		}
		finally{
			cli.close();
			ecrivain.close();
		}
	}
	public static void main(String[] args){
		try{
			MySQLToText.mySQLToText("article_stems","stems.txt");
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
}
