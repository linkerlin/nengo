/*
 * Created on 15-Jun-2006
 */
package ca.nengo.plot;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import ca.nengo.dynamics.Integrator;
import ca.nengo.dynamics.impl.EulerIntegrator;
import ca.nengo.dynamics.impl.LTISystem;
import ca.nengo.dynamics.impl.SimpleLTISystem;
import ca.nengo.math.Function;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.plot.impl.DefaultPlotter;
import ca.nengo.util.Environment;
import ca.nengo.util.SpikePattern;
import ca.nengo.util.TimeSeries;

/** 
 * Factory for frequently-used plots. 
 * 
 * @author Bryan Tripp
 */
public abstract class Plotter {
	
	private static Plotter ourInstance;
	
	private List<Frame> myPlotFrames;
	
	public Plotter() {
		myPlotFrames = new ArrayList<Frame>(10);
	}
	
	private synchronized static Plotter getInstance() {
		if (ourInstance == null) {
			//this can be made configurable if we get more plotters
			ourInstance = new DefaultPlotter(); 
		}
		
		return ourInstance;
	}

	/**
	 * Display a new plot. 
	 * 
	 * @param plotPanel A panel containng the plot image
	 * @param title The plot title 
	 */
	public void showPlot(JPanel plotPanel, String title) {
		final JFrame frame = new JFrame(title);
		frame.getContentPane().add(plotPanel, BorderLayout.CENTER);
		myPlotFrames.add(frame);
		
		try {
			Image image = ImageIO.read(this.getClass().getClassLoader().getResource("ca/nengo/plot/spikepattern-grey.png"));
			frame.setIconImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final Plotter plotter = this;
        frame.addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent e) {
        		plotter.closeAndDiscard(frame);
            }
        });

        frame.pack();
        frame.setVisible(true);		
	}
	
	private void closeAndDiscard(Frame plotFrame) {
		closePlot(plotFrame);
		myPlotFrames.remove(plotFrame);
		
		if (myPlotFrames.size() == 0 && !Environment.inUserInterface()) {
			System.exit(0); 
		}
	}

	//this part is separated from above to allow discarding via iterator in closeAll() 
	private void closePlot(Frame plotFrame) {
		if (plotFrame.isVisible()) {
			plotFrame.setVisible(false);
		}		
		plotFrame.dispose();
	}

	/**
	 * Close all open plots
	 */
	public static void closeAll() {
		getInstance().doCloseAll();
	}
	
	private void doCloseAll() {
		Iterator<Frame> it = myPlotFrames.iterator();
		while (it.hasNext()) {
			closePlot(it.next());		
			it.remove();
		}
	}
	
	/**
	 * Static convenience method for producing a TimeSeries plot.
	 *  
	 * @param series TimeSeries to plot
	 * @param title Plot title
	 */
	public static void plot(TimeSeries series, String title) {
		getInstance().doPlot(series, title);
	}
	
	/**
	 * As plot(TimeSeries) but series is filtered before plotting. This is useful when plotting 
	 * NEFEnsemble output (which may consist of spikes) in a manner more similar to the way it would 
	 * appear within post-synaptic neurons. 
	 * 
	 * @param series TimeSeries to plot
	 * @param tauFilter Time constant of display filter (s) 
	 * @param title Plot title
	 */
	public static void plot(TimeSeries series, float tauFilter, String title) {
		series = filter(series, tauFilter);
		getInstance().doPlot(series, title);
	}
	
	/**
	 * @param series A TimeSeries to which to apply a 1-D linear filter
	 * @param tauFilter Filter time constant
	 * @return Filtered TimeSeries
	 */
	public static TimeSeries filter(TimeSeries series, float tauFilter) {
		Integrator integrator = new EulerIntegrator(.0005f);
		
		int dim = series.getDimension();
		float[] A = new float[dim];
		float[][] B = new float[dim][];
		float[][] C = new float[dim][];
		for (int i = 0; i < dim; i++) {
			A[i] = -1f / tauFilter;
			B[i] = new float[dim];
			B[i][i] = 1f;
			C[i] = new float[dim];
			C[i][i] = 1f / tauFilter;
		}		
		LTISystem filter = new SimpleLTISystem(A, B, C, new float[dim], series.getUnits());
		
		return integrator.integrate(filter, series);		
	}

	/**
	 * Plots ideal and actual TimeSeries' together. 
	 *  
	 * @param ideal Ideal time series 
	 * @param actual Actual time series
	 * @param title Plot title
	 */
	public static void plot(TimeSeries ideal, TimeSeries actual, String title) {
		getInstance().doPlot(ideal, actual, title);
	}
	
	/**
	 * Plots multiple TimeSeries and/or SpikePatterns together in the same plot.
	 *   
	 * @param series A list of TimeSeries to plot (can be null if none)  
	 * @param patterns A list of SpikePatterns to plot (can be null if none)
	 * @param title Plot title
	 */
	public static void plot(List<TimeSeries> series, List<SpikePattern> patterns, String title) {
		getInstance().doPlot(series, patterns, title);
	}

	/**
	 * Plots ideal and actual TimeSeries' together, with each series filtered before plotting. 
	 * 
	 * @param ideal Ideal time series 
	 * @param actual Actual time series
	 * @param tauFilter Time constant of display filter (s) 
	 * @param title Plot title
	 */
	public static void plot(TimeSeries ideal, TimeSeries actual, float tauFilter, String title) {
		//ideal = filter(ideal, tauFilter);
		actual = filter(actual, tauFilter);
		getInstance().doPlot(ideal, actual, title);
	}
	
	/**
	 * @param series TimeSeries to plot
	 * @param title Plot title
	 */
	public abstract void doPlot(TimeSeries series, String title);
	
	/**
	 * @param ideal Ideal time series 
	 * @param actual Actual time series 
	 * @param title Plot title
	 */
	public abstract void doPlot(TimeSeries ideal, TimeSeries actual, String title);
	
	/**
	 * @param series A list of TimeSeries to plot (can be null if none)  
	 * @param patterns A list of SpikePatterns to plot (can be null if none)
	 * @param title Plot title
	 */
	public abstract void doPlot(List<TimeSeries> series, List<SpikePattern> patterns, String title);
	
	/**
	 * Static convenience method for producing a decoding error plot of an NEFEnsemble origin. 
	 * 
	 * @param ensemble NEFEnsemble from which origin arises
	 * @param origin Name of origin (must be a DecodedOrigin, not one derived from a combination of 
	 * 		neuron origins)
	 */
	public static void plot(NEFEnsemble ensemble, String origin) {
		getInstance().doPlot(ensemble, origin);
	}
	
	/**
	 * @param ensemble NEFEnsemble from which origin arises
	 * @param origin Name of origin (must be a DecodedOrigin, not one derived from a combination of 
	 * 		neuron origins)
	 */
	public abstract void doPlot(NEFEnsemble ensemble, String origin);
	
	/**
	 * Static convenience method for producing a plot of CONSTANT_RATE responses over range 
	 * of inputs. 
	 *  
	 * @param ensemble An NEFEnsemble  
	 */
	public static void plot(NEFEnsemble ensemble) {
		getInstance().doPlot(ensemble);
	}
	
	/**
	 * @param ensemble An NEFEnsemble  
	 */
	public abstract void doPlot(NEFEnsemble ensemble);
	
	/**
	 * Static convenience method for plotting a spike raster. 
	 * 
	 * @param pattern SpikePattern to plot
	 */
	public static void plot(SpikePattern pattern) {
		getInstance().doPlot(pattern);
	}
	
	/**
	 * @param pattern A SpikePattern for which to plot a raster
	 */
	public abstract void doPlot(SpikePattern pattern);

	/**
	 * Static convenience method for plotting a Function. 
	 * 
	 * @param function Function to plot
	 * @param start Minimum of input range 
	 * @param increment Size of incrememnt along input range 
	 * @param end Maximum of input range
	 * @param title Display title of plot
	 */
	public static void plot(Function function, float start, float increment, float end, String title) {
		getInstance().doPlot(function, start, increment, end, title);
	}
	
	/**
	 * @param function Function to plot
	 * @param start Minimum of input range 
	 * @param increment Size of incrememnt along input range 
	 * @param end Maximum of input range
	 * @param title Display title of plot
	 */
	public abstract void doPlot(Function function, float start, float increment, float end, String title);
	
	/**
	 * Static convenience method for plotting a vector. 
	 * 
	 * @param vector Vector of points to plot
	 * @param title Display title of plot
	 */
	public static void plot(float[] vector, String title) {
		getInstance().doPlot(vector, title);
	}
	
	/**
	 * @param vector Vector of points to plot
	 * @param title Display title of plot
	 */
	public abstract void doPlot(float[] vector, String title);
	
	/**
	 * Static convenience method for plotting a vector. 
	 *
	 * @param domain Vector of domain values 
	 * @param vector Vector of range values
	 * @param title Display title of plot
	 */
	public static void plot(float[] domain, float[] vector, String title) {
		getInstance().doPlot(domain, vector, title);
	}
	
	/**
	 * @param domain Vector of domain values 
	 * @param vector Vector of range values
	 * @param title Display title of plot
	 */
	public abstract void doPlot(float[] domain, float[] vector, String title);
	
}