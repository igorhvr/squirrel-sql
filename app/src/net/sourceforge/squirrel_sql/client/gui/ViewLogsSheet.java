package net.sourceforge.squirrel_sql.client.gui;
/*
 * Copyright (C) 2002-2004 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import net.sourceforge.squirrel_sql.fw.gui.CursorChanger;
import net.sourceforge.squirrel_sql.fw.gui.DirectoryListComboBox;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.gui.TextPopupMenu;
import net.sourceforge.squirrel_sql.fw.gui.ToolBar;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
/**
 * This sheet shows the SQuirreL log files.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ViewLogsSheet extends BaseInternalFrame
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(ViewLogsSheet.class);

	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(ViewLogsSheet.class);

	/** Singleton instance of this class. */
	private static ViewLogsSheet s_instance;

	/** Application API. */
	private final IApplication _app;

	/** Combo box containing all the log files. */
	private final LogsComboBox _logDirCmb = new LogsComboBox();

	/** Text area containing the log contents. */
	private final JTextArea _logContentsTxt = new JTextArea(20, 50);

	/** Button that refreshes the log contents. */
	private final JButton _refreshBtn = new JButton(s_stringMgr.getString("ViewLogsSheet.refresh"));

	/** Directory containing the log files. */
	private final File _logDir;

	/** If <TT>true</TT> user is closing this window. */
	private boolean _closing = false;

	/** If <TT>true</TT> log is being refreshed. */
	private boolean _refreshing = false;

	/**
	 * Ctor specifying the application API.
	 *
	 * @param	app		Application API.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>IApplication passed.
	 */
	private ViewLogsSheet(IApplication app)
	{
		super(s_stringMgr.getString("ViewLogsSheet.title"), true, true, true, true);
		if (app == null)
		{
			throw new IllegalArgumentException("IApplication == null");
		}

		_app = app;
		_logDir = new ApplicationFiles().getExecutionLogFile().getParentFile();
		createUserInterface();
	}

	/**
	 * Show this window
	 *
	 * @param	app		Application API.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>IApplication</TT> object passed.
	 */
	public static synchronized void showSheet(IApplication app)
	{
		if (s_instance == null)
		{
			s_instance = new ViewLogsSheet(app);
			app.getMainFrame().addInternalFrame(s_instance, true, null);
			GUIUtils.centerWithinDesktop(s_instance);
		}

		final boolean wasVisible = s_instance.isVisible();
		if (!wasVisible)
		{
			s_instance.setVisible(true);
		}
		s_instance.moveToFront();
		if (!wasVisible && !s_instance._refreshing)
		{
			s_instance.startRefreshingLog();
		}
	}

	public void dispose()
	{
		// Stop refresh if it is running.
		_closing = true;

		synchronized (getClass())
		{
			s_instance = null;
		}
		super.dispose();
	}

	/**
	 * Close this sheet.
	 */
	private void performClose()
	{
		dispose();
	}

	/**
	 * Start a thread to refrsh the log.
	 */
	private synchronized void startRefreshingLog()
	{
		if (!_refreshing)
		{
			_app.getThreadPool().addTask(new Refresher());
		}
	}

    /**
     * Enables the log combo box and refresh button using the Swing event 
     * thread.
     */
    private void enableComponents(final boolean enabled) 
    {
        GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
                _refreshBtn.setEnabled(enabled);
                _logDirCmb.setEnabled(enabled);
            }
        });
    }
    
	/**
	 * Refresh the log.
	 */
	private void refreshLog()
	{
	    enableComponents(false);
        CursorChanger cursorChg = new CursorChanger(this);
		cursorChg.show();
		try
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						_logContentsTxt.setText("");
					}
				});
			}
			catch (Exception ex)
			{
				s_log.error("Error", ex);
			}
			final File logFile = (File)_logDirCmb.getSelectedItem();
			if (logFile != null)
			{
				try
				{
					if (logFile.exists() && logFile.canRead())
					{
						final BufferedReader rdr = new BufferedReader(new FileReader(logFile));
						try
						{
							String line = null;
							StringBuffer chunk = new StringBuffer(16384);
							while ((line = rdr.readLine()) != null)
							{
								if (_closing)
								{
									return;
								}

								if (chunk.length() > 16000)
								{
									final String finalLine = chunk.toString();
									SwingUtilities.invokeAndWait(new Runnable()
									{
										public void run()
										{
											if (!_closing)
											{
												_logContentsTxt.append(finalLine);
											}
										}
									});
									chunk = new StringBuffer(16384);
								}
								else
								{
									chunk.append(line).append('\n');
								}
							}

							if (_closing)
							{
								return;
							}

							final String finalLine = chunk.toString();
							SwingUtilities.invokeAndWait(new Runnable()
							{
								public void run()
								{
									if (!_closing)
									{
										_logContentsTxt.append(finalLine);
									}
								}
							});
						}
						finally
						{
							rdr.close();
						}
					}
				}
				catch (Exception ex)
				{
					final String msg = "Error occured processing log file";
					s_log.error(msg, ex);
				}
			}
			else
			{
				s_log.debug("Null log file name");
			}

			if (_closing)
			{
				return;
			}

			// Position to the start of the last line in log.
			try
			{
				int pos = _logContentsTxt.getText().length() - 1;
				int line = _logContentsTxt.getLineOfOffset(pos);
				pos = _logContentsTxt.getLineStartOffset(line);
				_logContentsTxt.setCaretPosition(pos);
			}
			catch (BadLocationException ex)
			{
				s_log.error("Error positioning caret in log text component", ex);
			}
		}
		finally
		{
            enableComponents(true);
			_refreshing = false;
			cursorChg.restore();
		}
	}

	/**
	 * Create user interface.
	 */
	private void createUserInterface()
	{
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GUIUtils.makeToolWindow(this, true);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(createToolBar(), BorderLayout.NORTH);
		contentPane.add(createMainPanel(), BorderLayout.CENTER);
		contentPane.add(createButtonsPanel(), BorderLayout.SOUTH);
		pack();
	}

	private ToolBar createToolBar()
	{
		final ToolBar tb = new ToolBar();
		tb.setUseRolloverButtons(true);
		tb.setFloatable(false);

		final Object[] args = {getTitle(), _logDir.getAbsolutePath()};
		final String lblTitle = s_stringMgr.getString("ViewLogsSheet.storedin", args);
		final JLabel lbl = new JLabel(lblTitle);
		lbl.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		tb.add(lbl);

		return tb;
	}

	/**
	 * Create the main panel containing the log details and selector.
	 */
	private JPanel createMainPanel()
	{
		_logContentsTxt.setEditable(false);
		final TextPopupMenu pop = new TextPopupMenu();
		pop.setTextComponent(_logContentsTxt);
		_logContentsTxt.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent evt)
			{
				if (evt.isPopupTrigger())
				{
					pop.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
			public void mouseReleased(MouseEvent evt)
			{
				if (evt.isPopupTrigger())
				{
					pop.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		});

		File appLogFile = new ApplicationFiles().getExecutionLogFile();
		_logDirCmb.load(appLogFile.getParentFile());

		if (_logDirCmb.getModel().getSize() > 0)
		{
			_logDirCmb.setSelectedItem(appLogFile.getName());
		}

		// Done after the set of the selected item above so that we control
		// when the initial build is done. We want to make sure that under all
		// versions of the JDK that the window is shown before the (possibly
		// lengthy) refresh starts.
		_logDirCmb.addActionListener(new ChangeLogListener());

		final JPanel pnl = new JPanel(new BorderLayout());
		pnl.add(_logDirCmb, BorderLayout.NORTH);
		_logContentsTxt.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
		pnl.add(new JScrollPane(_logContentsTxt), BorderLayout.CENTER);

		return pnl;
	}

	/**
	 * Create panel at bottom containing the buttons.
	 */
	private JPanel createButtonsPanel()
	{
		JPanel pnl = new JPanel();

		pnl.add(_refreshBtn);
		_refreshBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				startRefreshingLog();
			}
		});

		JButton closeBtn = new JButton(s_stringMgr.getString("ViewLogsSheet.close"));
		closeBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				performClose();
			}
		});
		pnl.add(closeBtn);

		GUIUtils.setJButtonSizesTheSame(new JButton[] {closeBtn, _refreshBtn});
		getRootPane().setDefaultButton(closeBtn);

		return pnl;
	}

	private final class ChangeLogListener implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			ViewLogsSheet.this.startRefreshingLog();
		}
	}

	private final class Refresher implements Runnable
	{
		public void run()
		{
			ViewLogsSheet.this.refreshLog();
		}
	}

	private static final class LogsComboBox extends DirectoryListComboBox
	{
		private File _dir;

		public void load(File dir, FilenameFilter filter)
		{
			_dir = dir;
			super.load(dir, filter);
		}

		public void addItem(Object anObject)
		{
			super.addItem(new LogFile(_dir, anObject.toString()));
		}
	}

	private static final class LogFile extends File
	{
		private final String _stringRep;

		LogFile(File dir, String name)
		{
			super(dir, name);
			StringBuffer buf = new StringBuffer();
			buf.append(getName()).append(" (")
				.append(Utilities.formatSize(length())).append(")");
			_stringRep = buf.toString();
		}

		public String toString()
		{
			return _stringRep;
		}
	}
}
