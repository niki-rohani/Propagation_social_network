package elie.dataGenerator.Representations;
import java.util.ArrayList;

import elie.dataGenerator.tools.*;



public class ItemSet extends ArrayList<Item>{
	
	public int getNext(Representation userRep, int item){
		
		return toolbox.pickOne(userRep, this.get(item).getSuccessors(), this);
	}
}