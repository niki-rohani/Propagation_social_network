package niki.clustering.node;

import java.util.HashMap;

import niki.tool.UsersTool;
import core.Data;
import core.Text;



/**
 * Represente un data pour User cascade
 * @author dantidot
 *
 */
public class DataUser extends Data {

	
	public DataUser (HashMap <Integer, Text> users) {
		super (users, w(users.size()));
	}
	
	public static HashMap <Integer, Double> w (int size) {
		HashMap <Integer, Double> w = new HashMap <Integer, Double>();
		for (int u=0;u<size;u++) {
			w.put (u, 1.);
		}
		return w;
	}
	public HashMap <Integer, Text> getUsers() {
		return this.getTexts();
	}
	
	public UserByCascadeText getUser (int user) {
		return (UserByCascadeText)this.getText(user);
	}
	
}
