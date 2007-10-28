package ca.neo.ui.configurable.descriptors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ca.neo.math.Function;
import ca.neo.math.impl.ConstantFunction;
import ca.neo.math.impl.DefaultFunctionInterpreter;
import ca.neo.math.impl.FourierFunction;
import ca.neo.math.impl.GaussianPDF;
import ca.neo.ui.configurable.ConfigException;
import ca.neo.ui.configurable.PropertySet;
import ca.neo.ui.configurable.PropertyDescriptor;
import ca.neo.ui.configurable.IConfigurable;

/**
 * Config Descriptor for Functions
 * 
 * @author Shu Wu
 */
public class PFunction extends PropertyDescriptor {

	private static final ConfigurableFunction constantFunction = new ConfigurableFunction(
			ConstantFunction.class, "Constant Function",
			new PropertyDescriptor[] { new PInt("Dimension"),
					new PFloat("Value") });

	private static final ConfigurableFunction fourierFunction = new ConfigurableFunction(
			FourierFunction.class,
			"Fourier Function",
			new PropertyDescriptor[] { new PFloat("Fundamental"),
					new PFloat("Cutoff"), new PFloat("RMS"), new PLong("Seed") });

	private static final ConfigurableFunction gaussianPDF = new ConfigurableFunction(
			GaussianPDF.class, "Guassiand PDF", new PropertyDescriptor[] {
					new PFloat("Mean"), new PFloat("Variance"),
					new PFloat("Peak") });

	private static final ConfigurableFunction interpreterFunction = new ConfigurableFunction(
			DefaultFunctionInterpreter.class, "Default Interpreter Function",
			new PropertyDescriptor[] { new PString("Expression"),
					new PInt("Dimensions") });

	private static final long serialVersionUID = 1L;

	/**
	 * A list of configurable function wrappers
	 */
	public static final ConfigurableFunction[] functions = new ConfigurableFunction[] {
			constantFunction, fourierFunction, gaussianPDF, interpreterFunction };

	public PFunction(String name) {
		super(name);
	}

	public PFunction(String name, Object defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public FunctionPanel createInputPanel() {
		return new FunctionPanel(this);
	}

	@Override
	public Class<Function> getTypeClass() {
		return Function.class;
	}

	@Override
	public String getTypeName() {
		return "Function";
	}

}

/**
 * Describes how to configure a function through the IConfigurable interface.
 * Function instances are created through reflection.
 * 
 * @author Shu Wu
 */
class ConfigurableFunction implements IConfigurable {

	/**
	 * Function to be created
	 */
	private Function function;

	@SuppressWarnings("unchecked")
	private Class functionClass;

	/**
	 * A list of parameters required to create this function
	 */
	private PropertyDescriptor[] propStruct;

	/**
	 * What the type of function to be created is called
	 */
	private String typeName;

	/**
	 * @param functionClass
	 *            Class the function to be created belongs to
	 * @param typeName
	 *            Name of the type the function
	 * @param propStruct
	 *            A list of parameters referencing the constructor parameters
	 *            required to create the function
	 */
	public ConfigurableFunction(Class<?> functionClass, String typeName,
			PropertyDescriptor[] propStruct) {
		super();

		this.functionClass = functionClass;
		this.propStruct = propStruct;
		this.typeName = typeName;
	}

	/**
	 * @param function
	 *            New Function
	 */
	protected void setFunction(Function function) {
		this.function = function;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.neo.ui.configurable.IConfigurable#completeConfiguration(ca.neo.ui.configurable.ConfigParam)
	 *      Creates the function through reflection of its constructor and
	 *      passing the user parameters to it
	 */
	@SuppressWarnings("unchecked")
	public void completeConfiguration(PropertySet props) throws ConfigException {

		PropertyDescriptor[] metaProperties = getConfigSchema();

		/*
		 * Create function using Java reflection, function parameter are
		 * configured via the IConfigurable interface
		 */
		Class<?> partypes[] = new Class[metaProperties.length];
		for (int i = 0; i < metaProperties.length; i++) {

			partypes[i] = metaProperties[i].getTypeClass();

		}
		Constructor<?> ct = null;
		try {
			ct = getFunctionType().getConstructor(partypes);

			Object arglist[] = new Object[metaProperties.length];
			for (int i = 0; i < metaProperties.length; i++) {
				arglist[i] = props.getProperty(metaProperties[i].getName());
			}
			Object retobj = null;
			try {
				retobj = ct.newInstance(arglist);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			setFunction((Function) retobj);

		} catch (SecurityException e) {
			e.printStackTrace();
			throw new ConfigException("Could not configure function: "
					+ e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new ConfigException(
					"Could not configure function, no suitable constructor found");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.neo.ui.configurable.IConfigurable#getConfigSchema()
	 */
	public PropertyDescriptor[] getConfigSchema() {
		return propStruct;
	}

	/**
	 * @return The function created
	 */
	public Function getFunction() {
		return function;
	}

	/**
	 * @return The Class type the function belongs to
	 */
	@SuppressWarnings("unchecked")
	public Class getFunctionType() {
		return functionClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.neo.ui.configurable.IConfigurable#getTypeName()
	 */
	public String getTypeName() {
		return typeName;
	}

	@Override
	public String toString() {
		return getTypeName();
	}

}