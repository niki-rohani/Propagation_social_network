package wordsTreatment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import actionsBD.MySQLConnection;

public abstract class WeightComputer {
	protected String dbName;
	protected String colName;
	
	
	public WeightComputer(){
		this("propagation","stems");
	}
	public WeightComputer(String dbName,String colName){
		this.dbName=dbName;
		this.colName=colName;
	}
	
	public abstract HashMap<String,Double> getWeightsForStems(String st);
	public abstract HashMap<Integer,Double> getWeightsForIds(String st);
	
	public abstract String toString();
}
