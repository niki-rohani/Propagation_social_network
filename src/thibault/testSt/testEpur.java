package thibault.testSt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class testEpur {

	public static void main(String[] args) throws IOException {
		//Creer un json a partir d'une collection


			 FileWriter fw = new FileWriter("/data/lip6/db/bertinFinal.json");
			 BufferedWriter out = new BufferedWriter(fw);
			
			
	        // Fait une collection a partir d'un json
		

			
			//lecture du fichier texte	

				InputStream ips=new FileInputStream("/data/lip6/db/bertinTweets.json"); 
				InputStreamReader ipsr=new InputStreamReader(ips);
				BufferedReader br=new BufferedReader(ipsr);
				String ligne;
				while ((ligne=br.readLine())!=null){
					ligne=ligne.replaceAll("\t"," ");
					ligne=ligne.replaceAll("\n"," ");
					/*ligne=ligne.replace("ObjectId(", "");
					ligne=ligne.replace("ISODate(", "");
					ligne=ligne.replace("NumberLong(", "");
					ligne=ligne.replace(")", "");*/
					if(ligne.charAt(0)=='}'){
						out.write(ligne+"\n");
					}
					else{
						out.write(ligne);
					}
					 

				}
				br.close();
				out.close();

	}

}
