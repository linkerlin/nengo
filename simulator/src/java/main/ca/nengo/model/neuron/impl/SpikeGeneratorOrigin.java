/*
 * Created on May 23, 2006
 */
package ca.nengo.model.neuron.impl;

import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.SimulationException;
import ca.nengo.model.Units;
import ca.nengo.model.impl.SpikeOutputImpl;
import ca.nengo.model.neuron.Neuron;
import ca.nengo.model.neuron.SpikeGenerator;

/**
 * An Origin that obtains output from an underlying SpikeGenerator. This is a good Origin to use as 
 * the main (axonal) output of a spiking neuron. This Origin may produce SpikeOutput or RealOutput 
 * depending on whether it is running in DEFAULT or CONSTANT_RATE SimulationMode.  
 * 
 * @author Bryan Tripp
 */
public class SpikeGeneratorOrigin implements Origin {

	private static final long serialVersionUID = 1L;
	
	private Node myNode;
	private SpikeGenerator myGenerator;
	private InstantaneousOutput myOutput;
	
	/**
	 * @param node The parent Node
	 * @param generator The SpikeGenerator from which this Origin is to obtain output.  
	 */
	public SpikeGeneratorOrigin(Node node, SpikeGenerator generator) {
		myNode = node;
		myGenerator = generator;
		myOutput = new SpikeOutputImpl(new boolean[]{false}, Units.SPIKES, 0);
	}
	
	/**
	 * @return Neuron.AXON
	 * @see ca.nengo.model.Origin#getName()
	 */
	public String getName() {
		return Neuron.AXON;
	}

	/**
	 * @return 1
	 * @see ca.nengo.model.Origin#getDimensions()
	 */
	public int getDimensions() {
		return 1;
	}
	
	/**
	 * @param times Passed on to the run() or runConstantRate() method of the wrapped SpikeGenerator
	 * 		depending on whether the SimulationMode is DEFAULT or CONSTANT_RATE (in the latter case 
	 * 		only the first value is used).  
	 * @param current Passed on like the times argument. 
	 * @throws SimulationException Arising From the underlying SpikeGenerator, or if the given times 
	 * 		or values arrays have length 0 when in CONSTANT_RATE mode (the latter because the first 
	 * 		entries must be extracted). 
	 */
	public void run(float[] times, float[] current) throws SimulationException {
		myOutput = myGenerator.run(times, current);
	}

	/**
	 * Returns spike values or real-valued spike rate values, depending on whether the mode
	 * is SimulationMode.DEFAULT or SimulationMode.CONSTANT_RATE.  
	 * 
	 * @see ca.nengo.model.Origin#getValues()
	 */
	public InstantaneousOutput getValues() {
		return myOutput;
	}

	/**
	 * @see ca.nengo.model.Origin#getNode()
	 */
	public Node getNode() {
		return myNode;
	}

	@Override
	public Origin clone() throws CloneNotSupportedException {
		SpikeGeneratorOrigin result = (SpikeGeneratorOrigin) super.clone();
		result.myOutput = myOutput.clone();
		return result;
	}

}