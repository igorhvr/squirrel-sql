package net.sourceforge.squirrel_sql.client.plugin;

public class PluginStatus
{
	 /** Identifies the plugin that this object refers to. */
	private String _internalName;

	/** If tue plugin should be loaded at startup. */
	private boolean _loadAtStartup = true;

	public PluginStatus()
	{
		super();
	}

	public PluginStatus(String internalName)
	{
		super();
		_internalName = internalName;
	}

	/**
	 * Returns the name by which this plugin is uniquely identified.
	 *
	 * @return	the name by which this plugin is uniquely identified.
	 */
	public String getInternalName()
	{
		return _internalName;
	}

	public void setInternalName(String value)
	{
		_internalName = value;
	}

	public boolean isLoadAtStartup()
	{
		return _loadAtStartup;
	}

	public void setLoadAtStartup(boolean loadAtStartup)
	{
		_loadAtStartup = loadAtStartup;
	}
}
