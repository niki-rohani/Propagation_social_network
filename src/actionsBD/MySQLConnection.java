package actionsBD;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL client
 * @author Fobec 2010
 */
public class MySQLConnection {

    private MySQLCli cli=null;
    public static int nb_connections=0; 

    
    public MySQLConnection() {
        nb_connections++;
        cli=MySQLCli.getMySQLCli();
    }
  
    public void verbose(int v){
    	cli.verbose(v);
    }
    
    public boolean isValid(){
    	return(cli.isValid());
    }
    
       
    
    /**
     * Executer une requete SQL
     * @param sql
     * @return resultat de la requete
     */
    public ResultSet exec(String sql) {
        return(cli.exec(sql));
    }
    
    /**
     * Executer une requete SQL de modification
     * @param sql
     * 
     */
    public void update(String sql){ // throws com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
    	cli.update(sql);
    }

    /**
     * Fermer la connexion au serveur de DB
     */
    public void close() {
        if(cli!=null){
        	cli.release();
        	cli=null;
        	nb_connections--;
        }
    }

    public void finalize(){
    	close();
    }
    
}
