package thibault.simulationBandit;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class testWriter {

	public static void main(String[] args) throws IOException {
		
		String fichier = "globSim.txt";
		 FileWriter fw = new FileWriter(fichier);
		 BufferedWriter out = new BufferedWriter(fw);
		 
		 out.write("timeStep"+"\t");
		 for (int i=0;i<9;i++){
			 out.write("Arm"+i+"\t");
		 }
		 
		 out.write("\n");
		 
		 for (int j=0;j<5;j++){
			 out.write(j+"\t");
			 for (int i=0;i<9;i++){
				 out.write(i+"\t");
			 }
			 out.write("\n");
		 }

		 out.close();
	}

}

