package ca.neo.ui.views.objects.properties;

public interface IPropertiesConfigurable extends INamedObject {
	public abstract PropertySchema[] getMetaProperties();
	public void setProperty(String name, Object value);
	public Object getProperty(String name);
	
	public void configurationComplete();
	public void configurationCancelled();
	
//	public String getTypeName();
}
