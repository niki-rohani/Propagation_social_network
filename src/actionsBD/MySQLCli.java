package actionsBD;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
//import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQLCli {
	private static ArrayList<MySQLCli> clis=null;
	public static final int nb_clis=10;
	private static ArrayList<Integer> dispos=null;
	private String dbURL = "";
    private String user = "";
    private String password = "";
    private java.sql.Connection dbConnect = null;
    private java.sql.Statement dbStatement = null;
    private int verbose=0;
    private int id;
    //public static int active=0;
    //private final int max_active=20;
    

    /**
     * Constructeur
     * @param url
     * @param user
     * @param password
     */
    private MySQLCli(String url, String user, String password) {
        this.dbURL = url;
        this.user = user;
        this.password = password;
        verbose=0;
        this.id=clis.size();
        //waitFreeConnection();
        connect();
    }

    private MySQLCli() {
        this.dbURL = "//132.227.204.6:3306/wiki";
        this.user = "root";
        this.password = "rocknroll";
        verbose=0;
        this.id=clis.size();
        //waitFreeConnection();
        connect();
        /*ResultSet rs=exec("show table status");
        try{
        if (rs!=null){
			while(rs.next()){
				System.out.println(rs.getString(1));
			}
		}
        }
        catch(Exception e){
        	System.out.println(e);
        }*/
    }
    
    /*private synchronized void waitFreeConnection(){
    	//System.out.println(active+" connections actives");
    	boolean bloc=false;
    	while (dispos.size()==0){
    		//System.out.println("Wait for free connection ("+active+" actives)");
    		try{Thread.sleep(10000) ;}
    		catch (InterruptedException e) {}
    		bloc=true;
    	}
    	if (bloc){
    		System.out.println("Debloquage MySQLCli "); // ("+active+" actives)");
    	}
    	//active++;
    }*/
    
    
    public synchronized static MySQLCli getMySQLCli(){
    	if (dispos==null){
    		initPools();
    	}
    	boolean bloc=false;
    	while (dispos.size()==0){
    		bloc=true;
    		System.out.println("Wait for free connection ("+MySQLConnection.nb_connections+" demandees, "+nb_clis+" autorisees)");
    		try{Thread.sleep(10000) ;}
    		catch (InterruptedException e) {}
    	}
    	if (bloc){
    		System.out.println("Debloquage 1 thread : "+MySQLConnection.nb_connections+" demandees, "+nb_clis+" autorisees");
    	}
    	int icli=dispos.remove(0);
    	while(clis.size()<=icli){
    		MySQLCli cli=new MySQLCli();
    		clis.add(cli);
    	}
    	MySQLCli ret=clis.get(icli);
    	boolean valid=ret.isValid();
    	if (!valid){
    		ret.connect();
    	}
    	return(ret);
    }
    
    
    private static void initPools(){
    	dispos=new ArrayList<Integer>();
		for(int i=0;i<nb_clis;i++){
			dispos.add(i);
		}
		clis=new ArrayList<MySQLCli>();
    }
    
    // libere le cient (on remet dans le pool des dispos)
    public void release(){
    	dispos.add(id);
    }
    
    public void verbose(int v){
    	verbose=v;
    }
    
    
    public boolean isValid(){
    	if (dbConnect==null){
    		return(false);
    	}
    	try{
    		return(dbConnect.isValid(2));
    	}
    	catch(SQLException ex){
    		Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
    	}
    	return(false);
    }
    
    public Boolean connect() {
    	return(connect(1));
    }
    
    /**
     * Connecter a la base de donnee
     * @return false en cas d'echec
     */
    public Boolean connect(int it) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            //System.out.println("pass "+this.password);
            this.dbConnect = DriverManager.getConnection("jdbc:mysql:" + this.dbURL, this.user, this.password);
            this.dbStatement = this.dbConnect.createStatement();
            //System.out.println("connection ok");
            return true;
        } 
        catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException ex){
        	Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        	System.out.println("Encore ce probleme de CommunicationsException ! ");
        	//it--;
        }
        catch (SQLException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
       
        if (it<10){
        	System.out.println("Tentative "+it+" echouee, nouvelle tentative");
        	try{
        		Thread.sleep(10000);
        	}catch(InterruptedException e){
        		System.out.println(e);
        	}
        	return(connect(it+1));
        }
        
        return false;
    }

    
    /** reconnecte si besoin
     *retourne false en cas d'echec
    */ 
    public boolean reconnect(){
    	if(!isValid()){
    		return(connect());
    	}
    	else{
    		return true;
    	}
    }
    
   
    
    /**
     * Executer une requete SQL
     * @param sql
     * @return resultat de la requete
     */
    public ResultSet exec(String sql) {
    	 
        try {
        	if (verbose>0){
        		System.out.println(sql);
        	}
        	ResultSet rs = this.dbStatement.executeQuery(sql);
            return rs;
        } catch (SQLException ex) {
           Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Executer une requete SQL de modification
     * @param sql
     * 
     */
    public void update(String sql){ // throws com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
    	
        try {
        	if (verbose>0){
        		System.out.println(sql);
        	}
        	this.dbStatement.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Fermer la connexion au serveur de DB
     */
    public void close() {
        try {
        	if(dbStatement!=null){
        		this.dbStatement.close();
        		this.dbStatement=null;
        	}
            
            if (dbConnect!=null){
            	//active--;
            	this.dbConnect.close();
            	this.dbConnect=null;
            }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //System.out.println("Connection fermee");
    }

    public void finalize(){
    	close();
    }
    
    
    /**
     * Exemple d'utilisation de la class
     * @param args
     */
    public static void main(String[] args) {
        MySQLCli mysqlCli = new MySQLCli("//localhost", "", "");
        if (mysqlCli.connect()) {
            try {
                ResultSet rs = mysqlCli.exec("SELECT * FROM table");
                if (rs != null) {
                    while (rs.next()) {
                        System.out.println("Valeur: " + rs.getString(1));
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(MySQLCli.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            System.out.println("Mysql connection failed !!!");
        }
        mysqlCli.close();
    }
}
