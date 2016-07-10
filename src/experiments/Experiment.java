package experiments;

import java.sql.ResultSet;
import java.util.ArrayList;
import core.Structure;
//import actionsBD.MySQLConnection;

public interface Experiment {
	
	public Result go(Structure struct);
	
	public String getDescription();
	
	
}

