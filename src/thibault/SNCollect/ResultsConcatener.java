package thibault.SNCollect;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;


public class ResultsConcatener {
	
	//lecture du fichier texte	
	public ArrayList<String> fichiers;
	int modelNumber ;
	int nbRewards;
	String path;

	public ResultsConcatener(int modelNumber,int nbRewards, String path) throws IOException{
		this.fichiers=new ArrayList<String>();
		this.modelNumber=modelNumber;
		this.nbRewards=nbRewards;
		this.path =path;

		
		for(int i=1;i<=nbRewards;i++){
			this.fichiers.add(path+"model_"+modelNumber+"_Reward"+i+".txt");
		}
	}
	
	
	public void concatenate() throws IOException, ParseException{
		String fichierOut = path+"model_"+modelNumber+"_Concatenation_"+nbRewards+"_Rewards.txt";
		FileWriter fw = new FileWriter(fichierOut);
		BufferedWriter out = new BufferedWriter(fw);
		String fichierOutMeans = path+"model_"+modelNumber+"Mean_"+nbRewards+"_Rewards.txt";
		FileWriter fwMeans = new FileWriter(fichierOutMeans);
		BufferedWriter outMeans = new BufferedWriter(fwMeans);
		int nbFile=0;
		int nbLigne=0;
		ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();

		for(String fichier:fichiers){
			InputStream ips=new FileInputStream(fichier);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String ligne;
			ArrayList<String> column=new ArrayList<String>();
			int i=0;
			while ((ligne=br.readLine())!=null){
				if (i<5){
					i++;
					continue;
				}
				String str[] =ligne.split("\t\t");

				if(nbFile==0){
					nbLigne++;
					String result0 =str[0].replace(" ","");
					String result1 =str[1].replace(" ","");
					String result=result0+"\t"+result1;
					column.add(result);
				}
				else{
					String result =str[1].replace(" ","");
					column.add(result);	
				}


			}
			br.close();
			matrix.add(column);
			nbFile ++;
		}
		
		out.write("Data\t");
		for(int i=1;i<=nbFile;i++)
		{
			out.write("Reward"+i+"\t");
		}
		out.write("\n");
		for(int j=0;j<nbLigne;j++){
			for(int i=0;i<nbFile;i++)
			{
				out.write(matrix.get(i).get(j)+"\t");
			}
			out.write("\n");
		}
		out.close();
		
		ArrayList<Double> means = new ArrayList<Double>();
		for(int j=0;j<nbLigne;j++){
			means.add(j, 0.0);
		}
		
		NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
		for(int j=0;j<nbLigne;j++){
			for(int i=0;i<nbFile;i++)
			{	
				Number number = format.parse(matrix.get(i).get(j));
				means.set(j, means.get(j) +number.doubleValue()/nbFile);
			}
		}
		
		for(int j=0;j<nbLigne;j++){
			outMeans.write(means.get(j)+"\n");
		}
		
		outMeans.close();
		
	}

	/*public static void main(String[] args) throws FileNotFoundException, Exception{
		ResultsConcatener concatener = new ResultsConcatener(1,39);
		concatener.concatenate();  
	}*/
}






/*class ResultsConcatenerAll {
	String path; //path of the folder containing all the subfolder in which there are the mean files
	public ArrayList<String> fichiers;
	int modelNumber ;
	int nbRewards;
	
	public ResultsConcatenerAll(int modelNumber,int nbRewards, String path) throws IOException{
		this.fichiers=new ArrayList<String>();
		this.path =path;
		this.modelNumber=modelNumber;
		this.nbRewards=nbRewards;
		
		File dir = new File(path);
		File listDir[] = dir.listFiles();
		for (int i = 0; i < listDir.length; i++) {
		    if (listDir[i].isDirectory()) {
		    	this.fichiers.add(path+listDir[i].getName()+"/model_"+modelNumber+"Mean_"+nbRewards+"_Rewards.txt"); 
		        }
		}
	}
	
	public void concatenate() throws IOException, ParseException{
		String fichierOut = path+"ConcatenationMoyenneRewards.txt";
		FileWriter fw = new FileWriter(fichierOut);
		BufferedWriter out = new BufferedWriter(fw);
		int nbFile=0;
		int nbLigne=0;
		ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();

		for(String fichier:fichiers){
			InputStream ips=new FileInputStream(fichier);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String ligne;
			ArrayList<String> column=new ArrayList<String>();
			int i=0;
			while ((ligne=br.readLine())!=null){

				if(nbFile==0){
					nbLigne++;
					column.add(ligne);
				}
				else{
					column.add(ligne);	
				}
			}
			br.close();
			matrix.add(column);
			nbFile ++;
		}
		

		
		for(int j=0;j<nbLigne;j++){
			for(int i=0;i<nbFile;i++)
			{
				out.write(matrix.get(i).get(j)+"\t");
			}
			out.write("\n");
		}
		out.close();

		
	}
	
	}*/