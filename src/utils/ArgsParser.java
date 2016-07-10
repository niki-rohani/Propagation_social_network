package utils;

import java.util.HashMap;

public class ArgsParser {

	/**
	 * Parses args of the input parameters table which contains strings formatted as "-x=y" and returns a map <x,y>.
	 * args not in format "-x=y" are ignored.
	 * @param args  table of strings formatted as "-x=y"
	 * @return  a map <x,y> 
	 */
	public static HashMap<String,String> parseArgs(String[] args){
		HashMap<String,String> ret=new HashMap<String,String>();
		for(String arg:args){
			if(!arg.startsWith("-")){
				continue;
			}
			if(!arg.contains("=")){
				continue;
			}
			arg=arg.substring(1);
			String[] sarg=arg.split("=");
			ret.put(sarg[0], sarg[1]);
			System.out.println(sarg[0]+" = "+sarg[1]);
		}
		return ret;
	}
	
}
