package niki.test;

import java.util.HashMap;
import java.util.Set;

import clustering.KernelKMeans;
import similarities.StrSim;
import core.Text;
import core.User;
import niki.clustering.JacquartSim;
import niki.tool.ClusterTool;
import niki.tool.UsersTool;
import core.Data;
import niki.clustering.*;
import niki.clustering.node.DataUser;
import niki.clustering.node.UserByCascadeText;
import niki.experiment.Experiment;

public class EvalIC {

    public static void run(int k, HashMap <Integer, Double> similaritie, String maxIter, String nbSim, double lambda, String cascade)
            {
    				if (cascade == "train")
    					cascade = "cascades_1";
    				else
    					cascade = "cascades_2";
             		String user = "";
             		String casc = "";
             		String db = "";
             		if (k == 0) {
            			user = "users_1";
            			db = "digg";
            			casc = cascade;
            		}
             		else {
             		user = "user_Kmean_"+Experiment.getStringSimW(similaritie)+"_k_"+k;
             		casc = cascade + "l" + user + ((lambda==0)?"":"_"+lambda);
             		db = "niki";
             		}
                     //String casc = "cascades_2"; // FAIRE cascades_1
                     // String user = "users_1";
                     String step = "1";
                     String likelihood = "false";
                     String[] argument = {db, casc, user, "ic/" + user /* + ((lambda==0)?"":"_"+lambda) */, "ic/result/" + user + ((lambda==0)?"":"_"+lambda), "0", maxIter, nbSim, step, likelihood};
                     simon.test.evalIC.main(argument);
		}

}
