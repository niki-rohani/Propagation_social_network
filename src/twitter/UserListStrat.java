package twitter;

import java.io.FileNotFoundException;
import java.io.IOException;


// Strategie de construction de liste d'utilisateur
public interface UserListStrat {
	
	// Creer une liste en la stockant quelque part
	public void createList(String fileName) throws IOException ;
	
	
}
