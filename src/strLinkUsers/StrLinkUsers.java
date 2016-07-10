package strLinkUsers;

import java.util.HashSet;
import cascades.Cascade;

import core.User;
//Strategie de mise en relation de users a partir d une collection de cascades
public abstract class StrLinkUsers {
	public abstract String linkUsers(String db,String col); 
}
