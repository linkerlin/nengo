/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "ALIFSpikeGenerator.java". Description: 
"An adapting leaky-integrate-and-fire model of spike generation"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU 
Public License license (the GPL License), in which case the provisions of GPL 
License are applicable  instead of those above. If you wish to allow use of your 
version of this file only under the terms of the GPL License and not to allow 
others to use your version of this file under the MPL, indicate your decision 
by deleting the provisions above and replace  them with the notice and other 
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 11-May-07
 */
package ca.nengo.model.neuron.impl;

import java.util.Properties;

import ca.nengo.math.Function;
import ca.nengo.math.PDF;
import ca.nengo.math.PDFTools;
import ca.nengo.math.RootFinder;
import ca.nengo.math.impl.AbstractFunction;
import ca.nengo.math.impl.IndicatorPDF;
import ca.nengo.math.impl.NewtonRootFinder;
import ca.nengo.math.impl.PiecewiseConstantFunction;
import ca.nengo.model.InstantaneousOutput;
import ca.nengo.model.Network;
import ca.nengo.model.Node;
import ca.nengo.model.Probeable;
import ca.nengo.model.SimulationException;
import ca.nengo.model.SimulationMode;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Units;
import ca.nengo.model.impl.EnsembleImpl;
import ca.nengo.model.impl.FunctionInput;
import ca.nengo.model.impl.NetworkImpl;
import ca.nengo.model.impl.PreciseSpikeOutputImpl;
import ca.nengo.model.impl.RealOutputImpl;
import ca.nengo.model.impl.SpikeOutputImpl;
import ca.nengo.model.neuron.SpikeGenerator;
import ca.nengo.plot.Plotter;
import ca.nengo.util.Probe;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.TimeSeries1D;
import ca.nengo.util.impl.TimeSeries1DImpl;

/**
 * <p>An adapting leaky-integrate-and-fire model of spike generation. The mechanism of adaptation is 
 * a current I_ahp that is related to firing frequency. This current is proportional to the 
 * concentration of an ion species N, as I_ahp = -g_N * [N]. [N] increases with every spike and 
 * decays between spikes, as follows: d[N]/dt = -[N]/tau_N + A_N sum_k(delta(t-t_k). </p> 
 * 
 * <p>This form is taken from La Camera et al. (2004) Minimal models of adapted neuronal response to in vivo-like
 * input currents, Neural Computation 16, 2101-24. This form of adaptation (as opposed to variation in firing 
 * threshold or membrane time constant) is convenient because it allows a rate model as well as a spiking model.</p>
 * 
 * TODO: unit tests (particularly verify numbers of spikes and rate match in various cases -- they seem to)
 * 
 * @author Bryan Tripp
 */
public class ALIFSpikeGenerator implements SpikeGenerator, Probeable {

	private static final long serialVersionUID = 1L;

	private static final SimulationMode[] mySupportedModes = new SimulationMode[]{SimulationMode.DEFAULT, SimulationMode.RATE, SimulationMode.CONSTANT_RATE};
	private static final float R = 1;
	private static final float Vth = 1;
//	private static final float G_N = 10;	
	private static final float G_N = 1;	
	
	private SimulationMode myMode = SimulationMode.DEFAULT;
	
	private float myTauRef;
	private float myTauRC;
	private float myTauN;
	private float myIncN; //increment of N with each spike
	private float myInitialVoltage = 0;
	
	private float myV;
	private float myN;	
	private float myTimeSinceLastSpike;
	
	private float[] myTime;
	private float[] myVHistory;
	private float[] myNHistory;
	private float[] myRateHistory;
	
	private static final float[] ourNullTime = new float[0]; 
	private static final float[] ourNullVHistory = new float[0];	
	private static final float[] ourNullNHistory = new float[0];	
	private static final float[] ourNullRateHistory = new float[0];	
	
	/**
	 * Uses default parameters
	 */
	public ALIFSpikeGenerator() {
		this(.002f, .02f, .2f, 0);
	}
	
	/**
	 * @param tauRef Refracory period (s)
	 * @param tauRC Resistive-capacitive time constant (s) 
	 * @param tauN Time constant of adaptation-related ion
	 * @param incN Increment of adaptation-related ion with each spike
	 */
	public ALIFSpikeGenerator(float tauRef, float tauRC, float tauN, float incN) {
		myTauRef = tauRef;
		myTauRC = tauRC;
		myTauN = tauN;
		setIncN(incN);
		reset(false);
	}

	/**
	 * @return Refracory period (s)
	 */
	public float getTauRef() {
		return myTauRef;
	}
	
	/**
	 * @param tauRef Refracory period (s)
	 */
	public void setTauRef(float tauRef) {
		myTauRef = tauRef;
	}

	/**
	 * @return Resistive-capacitive time constant (s) 
	 */
	public float getTauRC() {
		return myTauRC;
	}

	/**
	 * @param tauRC Resistive-capacitive time constant (s) 
	 */
	public void setTauRC(float tauRC) {
		myTauRC = tauRC;
	}

	/**
	 * @return Time constant of adaptation-related ion
	 */
	public float getTauN() {
		return myTauN;
	}

	/**
	 * @param tauN Time constant of adaptation-related ion
	 */
	public void setTauN(float tauN) {
		myTauN = tauN;
	}

	/**
	 * @return Increment of adaptation-related ion with each spike
	 */
	public float getIncN() {
		return myIncN;
	}

	/**
	 * @param incN Increment of adaptation-related ion with each spike
	 */
	public void setIncN(float incN) {
		myIncN = Math.max(0, incN); //TODO: rethink this (prevents potentiation)
	}

	/**
	 * @see ca.nengo.model.neuron.SpikeGenerator#run(float[], float[])
	 */
	public InstantaneousOutput run(float[] time, float[] current) {
		float I_in = (current[0] + current[current.length-1]) / 2;
		float dt = time[time.length-1] - time[0];

		float dN = - myN / myTauN;
		myN = Math.max(0, myN + dt*dN);
		
		float I = I_in - G_N*myN; 
		
		InstantaneousOutput result = null;
		if (myMode.equals(SimulationMode.DEFAULT) || myMode.equals(SimulationMode.PRECISE)) {
			myTimeSinceLastSpike = myTimeSinceLastSpike + dt;

			float dV = (1 / myTauRC) * (I*R - myV);			 
			if (myTimeSinceLastSpike < myTauRef) {
				dV = 0;
			} else if (myTimeSinceLastSpike < myTauRef+dt) {
				dV*=(myTimeSinceLastSpike-myTauRef)/dt;				
			}
			
			float prevV=myV;
			myV = Math.max(0, myV + dt*dV);

			float spikeTime=-1f;
			if (myV >= Vth) {
				spikeTime=(Vth-prevV)*dt/(myV-prevV);
				myTimeSinceLastSpike = dt-spikeTime;

				myN += myIncN; 
				myV = 0;		
			}
			
			myRateHistory = new float[]{spikeTime>=0 ? 1f/dt : 0};
			
			if (myMode.equals(SimulationMode.DEFAULT))
				result = new SpikeOutputImpl(new boolean[]{spikeTime>=0f}, Units.SPIKES, time[time.length-1]);
			else
				result = new PreciseSpikeOutputImpl(new float[]{spikeTime}, Units.SPIKES, time[time.length-1]);
		} else if (myMode.equals(SimulationMode.RATE)) {
			float rate = I > 1 ? 1f / ( myTauRef - myTauRC * ((float) Math.log(1f - 1f/I)) ) : 0;
			myN += (rate * dt) * myIncN; //analog of # spikes X increment 
			
			myRateHistory = new float[]{rate};
			result = new RealOutputImpl(new float[]{rate}, Units.SPIKES_PER_S, time[time.length-1]);
		} else {
			float rate = I_in > 1 ? 1f / ( myTauRef - myTauRC * ((float) Math.log(1f - 1f/I_in)) ) : 0;

			myRateHistory = new float[]{rate};
			result = new RealOutputImpl(new float[]{rate}, Units.SPIKES_PER_S, time[time.length-1]);
		}
		
		myTime = new float[]{time[time.length-1]};
		myNHistory = new float[]{myN};
		myVHistory = new float[]{myV};
		
		return result;
	}
	
	/**
	 * @param I driving current
	 * @return Unadapted firing rate given this current
	 */
	public float getOnsetRate(float I) {
		return I > 1 ? 1f / ( myTauRef - myTauRC * ((float) Math.log(1f - 1f/I)) ) : 0;
	}
	
	/**
	 * @param I driving current
	 * @return Adapted firing rate given this current
	 */
	public float getAdaptedRate(final float I) {
		if (I > 1) {
			RootFinder rf = new NewtonRootFinder(50, false);
			
			Function f = new AbstractFunction(1) {
				private static final long serialVersionUID = 1L;
				public float map(float[] from) {
					float r = from[0];
					return r - 1 / (myTauRef - myTauRC * (float) Math.log(1f - 1f/(I-G_N*myIncN*myTauN*r)));
				}
			};
			
			float max = (I-1) / (G_N * myIncN * myTauN); //will be NaN if current < 1
			return rf.findRoot(f, 0, max, 0.1f);
		} else {
			return 0;
		}
	}
	
	/**
	 * @see ca.nengo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		myTimeSinceLastSpike = myTauRef;
		myN = 0;
		myV = myInitialVoltage;		
		myTime = ourNullTime;
		myVHistory = ourNullVHistory;
		myNHistory = ourNullNHistory;
		myRateHistory = ourNullRateHistory;
	}

	/**
	 * @see ca.nengo.model.SimulationMode.ModeConfigurable#getMode()
	 */
	public SimulationMode getMode() {
		return myMode;
	}

	/**
	 * DEFAULT and RATE are supported. 
	 * 
	 * @see ca.nengo.model.SimulationMode.ModeConfigurable#setMode(ca.nengo.model.SimulationMode)
	 */
	public void setMode(SimulationMode mode) {
		myMode = SimulationMode.getClosestMode(mode, mySupportedModes);
	}

	/**
	 * @see ca.nengo.model.Probeable#getHistory(java.lang.String)
	 */
	public TimeSeries getHistory(String stateName) throws SimulationException {
		TimeSeries1D result = null;
		
		if (stateName.equals("V")) {
			result = new TimeSeries1DImpl(myTime, myVHistory, Units.AVU); 
		} else if (stateName.equalsIgnoreCase("N")) {
			result = new TimeSeries1DImpl(myTime, myNHistory, Units.UNK); 
		} else if (stateName.equalsIgnoreCase("rate")) {
			result = new TimeSeries1DImpl(myTime, myRateHistory, Units.SPIKES_PER_S); 
		} else {
			throw new SimulationException("The state name " + stateName + " is unknown.");
		}
		
		return result;
	}

	/**
	 * @see ca.nengo.model.Probeable#listStates()
	 */
	public Properties listStates() {
		Properties p = new Properties();
		p.setProperty("V", "Membrane potential (arbitrary units)");
		p.setProperty("N", "Concentration of adaptation-related ion (arbitrary units)");
		p.setProperty("rate", "Firing rate (only available in rate mode) (spikes/s)");
		return p;
	}
	
	@Override
	public SpikeGenerator clone() throws CloneNotSupportedException {
		ALIFSpikeGenerator result = (ALIFSpikeGenerator) super.clone();
		result.myNHistory = myNHistory.clone();
		result.myRateHistory = myRateHistory.clone();
		result.myTime = myTime.clone();
		result.myVHistory = myVHistory.clone();
		return result;
	}

	/**
	 * Creates ALIFSpikeGenerators. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Factory implements SpikeGeneratorFactory {

		private static final long serialVersionUID = 1L;
		
		private PDF myTauRef;
		private PDF myTauRC;
		private PDF myTauN;
		private PDF myIncN;

		public Factory() {
			myTauRef = new IndicatorPDF(.002f);
			myTauRC = new IndicatorPDF(.02f);
			myTauN = new IndicatorPDF(.2f);
			myIncN = new IndicatorPDF(.1f);
		}
		
		/**
		 * @return PDF of refractory periods (s)
		 */
		public PDF getTauRef() {
			return myTauRef;
		}
		
		/**
		 * @param tauRef PDF of refractory periods (s)
		 */
		public void setTauRef(PDF tauRef) {
			myTauRef = tauRef;
		}
		
		/**
		 * @return PDF of membrane time constants (s)
		 */
		public PDF getTauRC() {
			return myTauRC;
		}
		
		/**
		 * @param tauRC PDF of membrane time constants (s)
		 */
		public void setTauRC(PDF tauRC) {
			myTauRC = tauRC;
		}
		
		/**
		 * @return PDF of time constants of the adaptation variable (s)
		 */
		public PDF getTauN() {
			return myTauN;
		}
		
		/**
		 * @param tauN PDF of time constants of the adaptation variable (s)
		 */
		public void setTauN(PDF tauN) {
			myTauN = tauN;
		}
		
		/**
		 * @return PDF of increments of the adaptation variable
		 */
		public PDF getIncN() {
			return myIncN;
		}
		
		/**
		 * @param incN PDF of increments of the adaptation variable
		 */
		public void setIncN(PDF incN) {
			myIncN = incN;
		}
		
		/**
		 * @see ca.nengo.model.neuron.impl.SpikeGeneratorFactory#make()
		 */
		public SpikeGenerator make() {
			return new ALIFSpikeGenerator(myTauRef.sample()[0], myTauRC.sample()[0], myTauN.sample()[0], myIncN.sample()[0]);
		}	
	}
	
	
	//functional test
	public static void main(String[] args) {
		
		try {
			Network network = new NetworkImpl();
			
			//x, .3: varying x keeps time constant, changes adapted rate
//			ALIFSpikeGenerator generator = new ALIFSpikeGenerator(.002f, .02f, .5f, .01f);  //.2: .01 to .3 (150 to 20ms)
//			SynapticIntegrator integrator = new LinearSynapticIntegrator(.001f, Units.ACU);
//			PlasticExpandableSpikingNeuron neuron = new PlasticExpandableSpikingNeuron(integrator, generator, 15f, 0f, "alif");
			
			ALIFNeuronFactory factory = new ALIFNeuronFactory(new IndicatorPDF(200, 400), new IndicatorPDF(-2.5f, -1.5f), 
					new IndicatorPDF(.1f, .1001f), .0005f, .02f, .2f);

//			VectorGenerator vg = new RandomHypersphereVG(false, 1, 0);
//			ApproximatorFactory factory = new WeightedCostApproximator.Factory(.1f);
//			NEFEnsemble ensemble = new NEFEnsembleImpl("ensemble", new NEFNode[]{neuron}, new float[][]{new float[]{1}}, factory, vg.genVectors(100, 1));
			
			Node[] neurons = new Node[50];
			float[][] weights = new float[neurons.length][];
			for (int i = 0; i < neurons.length; i++) {
				neurons[i] = factory.make("neuron"+i);
				weights[i] = new float[]{1};
			}
			EnsembleImpl ensemble = new EnsembleImpl("ensemble", neurons);
			ensemble.addTermination("input", weights, .005f, false);
			ensemble.collectSpikes(true);
			network.addNode(ensemble);
			
			FunctionInput input = new FunctionInput("input", new Function[]{new PiecewiseConstantFunction(new float[]{0.2f}, new float[]{0, 0.5f})}, Units.UNK);
			network.addNode(input);
			
			network.addProjection(input.getOrigin(FunctionInput.ORIGIN_NAME), ensemble.getTermination("input"));
			
//			Probe vProbe = network.getSimulator().addProbe("ensemble", 0, "V", true);
//			Probe nProbe = network.getSimulator().addProbe("ensemble", 0, "N", true);
//			Probe iProbe = network.getSimulator().addProbe("ensemble", 0, "I", true);
			Probe rProbe = network.getSimulator().addProbe("ensemble", "rate", true);
			
			network.setMode(SimulationMode.RATE);			
			network.run(0, 1);
			
//			Plotter.plot(ensemble.getSpikePattern());
//			Plotter.plot(vProbe.getData(), "V");
//			Plotter.plot(nProbe.getData(), "N");
//			Plotter.plot(iProbe.getData(), "I");
			Plotter.plot(rProbe.getData(), "Rate");
			
		} catch (StructuralException e) {
			e.printStackTrace();
		} catch (SimulationException e) {
			e.printStackTrace();
		}
	}

}
