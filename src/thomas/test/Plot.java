package thomas.test;

import com.panayotis.gnuplot.JavaPlot;

public class Plot {

	public static void main(String[] args) {
		JavaPlot p = new JavaPlot(true);
        p.addPlot("sin(x)");
        p.plot();
	}

}
