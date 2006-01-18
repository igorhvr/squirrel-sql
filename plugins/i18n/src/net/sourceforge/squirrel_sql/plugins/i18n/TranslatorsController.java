package net.sourceforge.squirrel_sql.plugins.i18n;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.plugin.PluginInfo;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TranslatorsController
{
   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(TranslatorsController.class);

	private static ILogger logger = LoggerController.createLogger(TranslatorsController.class);


   private static final String PREF_KEY_WORK_DIR = "SquirrelSQL.i18n.workDir";
   private static final String PREF_KEY_EDITOR_COMMAND = "SquirrelSQL.i18n.editorCommand";
   private static final String PREF_KEY_SELECTED_LOCALE = "SquirrelSQL.i18n.selectedLocale";


   TranslatorsPanel _panel;
   private IApplication _app;
   private BundlesTableModel _bundlesTableModel;

   private JPopupMenu _popUp = new JPopupMenu();
   private JMenuItem _mnuGenerateTemplateComments = new JMenuItem(s_stringMgr.getString("I18n.generateTemplateComments"));
   // i18n[I18n.generateTemplateComments=Generate template comments for missing translations]

   private JMenuItem _mnuOpenInEditor = new JMenuItem(s_stringMgr.getString("I18n.openIOnEditor"));
   // i18n[I18n.openIOnEditor=Open in Editor]

   TranslatorsController(TranslatorsPanel panel)
   {
      _panel = panel;

      _bundlesTableModel = new BundlesTableModel();

      _panel.tblBundels.setModel(_bundlesTableModel);

      _panel.btnChooseWorkDir.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onChooseWorkDir();
         }
      });

      _panel.cboLocales.addItemListener(new ItemListener()
      {
         public void itemStateChanged(ItemEvent e)
         {
            onLocaleChanged(e);
         }
      });

      _panel.btnChooseEditorCommand.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onChooseEditorCommand();
         }
      });



      _panel.tblBundels.addMouseListener(new MouseAdapter()
      {
         public void mousePressed(MouseEvent e)
         {
            maybeShowPopup(e);
         }

         public void mouseReleased(MouseEvent e)
         {
            maybeShowPopup(e);
         }
      });

      _popUp.add(_mnuGenerateTemplateComments);
      _popUp.add(_mnuOpenInEditor);

      _mnuGenerateTemplateComments.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onGenerate();
         }
      });

      _mnuOpenInEditor.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onOpenInEditor();
         }
      });


      Locale[] availableLocales = Locale.getAvailableLocales();

      Arrays.sort(availableLocales, new Comparator()
      {
         public int compare(Object o1, Object o2)
         {
            return o1.toString().compareTo(o2.toString());
         }
      });


      Locale selectedLocale = Locale.getDefault();
      String prefLocale = Preferences.userRoot().get(PREF_KEY_SELECTED_LOCALE, null);

      for (int i = 0; i < availableLocales.length; i++)
      {
         _panel.cboLocales.addItem(availableLocales[i]);

         if(availableLocales[i].toString().equals(prefLocale))
         {
            selectedLocale = availableLocales[i];
         }
      }
      _panel.cboLocales.setSelectedItem(selectedLocale);

      _panel.btnLoad.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onLoadBundels(_app);
         }
      });

      String workDir = Preferences.userRoot().get(PREF_KEY_WORK_DIR, null);
      _panel.txtWorkingDir.setText(workDir);

      String editorCommand = Preferences.userRoot().get(PREF_KEY_EDITOR_COMMAND, null);
      _panel.txtEditorCommand.setText(editorCommand);

   }


   private void onChooseWorkDir()
   {
      JFileChooser chooser = new JFileChooser(System.getProperties().getProperty("user.home"));
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.showOpenDialog(_app.getMainFrame());

      if(null != chooser.getSelectedFile())
      {
         _panel.txtWorkingDir.setText(chooser.getSelectedFile().getPath());
      }
   }

   private void onLocaleChanged(ItemEvent e)
   {
      if(ItemEvent.SELECTED == e.getStateChange())
      {
         _bundlesTableModel.setBundles(new I18nBundle[0]);
      }
   }



   private void onChooseEditorCommand()
   {
      JFileChooser chooser = new JFileChooser(System.getProperties().getProperty("user.home"));
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.showOpenDialog(_app.getMainFrame());

      if(null != chooser.getSelectedFile())
      {
         _panel.txtEditorCommand.setText(chooser.getSelectedFile().getPath());
      }
   }

   private void onOpenInEditor()
   {
      File workDir = getWorkDir(true);

      String editorCommmand = _panel.txtEditorCommand.getText();

      if(null == editorCommmand || 0 == editorCommmand.length())
      {
         String msg = s_stringMgr.getString("i18n.noEditorCommand");
         // i18n[i18n.noEditorCommand=Can not open files withou an editor command.]
         JOptionPane.showMessageDialog(_app.getMainFrame(), msg);
      }

      int[] selRows = _panel.tblBundels.getSelectedRows();
      I18nBundle[] selBundles = _bundlesTableModel.getBundlesForRows(selRows);

      if(0 == selBundles.length)
      {
         return;
      }

      boolean filesFound = false;

      for (int i = 0; i < selBundles.length; i++)
      {
         File f = selBundles[i].getPathInWorkDir(workDir);

         if(f.exists())
         {
            editorCommmand += " " + f.getPath();
            filesFound = true;
         }
         else
         {
            String msg = s_stringMgr.getString("i18n.notGeneratedInWorkDir", f.getPath());
            // i18n[i18n.notGeneratedInWorkDir=File {0} has not been generated and can not be opened.]
            _app.getMessageHandler().showMessage(msg);
         }
      }

      if(false == filesFound)
      {
         String msg = s_stringMgr.getString("i18n.noFilesOpened");
         // i18n[i18n.noFilesOpened=No file could be opened.\nSee message panel for details.]
         JOptionPane.showMessageDialog(_app.getMainFrame(), msg);
         return;
      }

      String msg = s_stringMgr.getString("i18n.executingCommand", editorCommmand);
      // i18n[i18n.executingCommand=Executing command: {0}]
      _app.getMessageHandler().showMessage(msg);

      try
      {
         Runtime.getRuntime().exec(editorCommmand);
      }
      catch (IOException e)
      {
         msg = s_stringMgr.getString("i18n.executingCommandFailed", e.getMessage());
         // i18n[i18n.executingCommandFailed=Execution failed with error: {0}]
         _app.getMessageHandler().showMessage(msg);
         throw new RuntimeException(e);

      }
   }


   public void uninitialize()
   {
      Preferences.userRoot().put(PREF_KEY_WORK_DIR, _panel.txtWorkingDir.getText());
      Preferences.userRoot().put(PREF_KEY_EDITOR_COMMAND, _panel.txtEditorCommand.getText());
      Preferences.userRoot().put(PREF_KEY_SELECTED_LOCALE, "" + _panel.cboLocales.getSelectedItem());
   }


   private void maybeShowPopup(MouseEvent e)
   {
      if (e.isPopupTrigger())
      {
         _popUp.show(e.getComponent(), e.getX(), e.getY());
      }
   }


   private void onGenerate()
   {
      File workDir = getWorkDir(true);

      if(null == workDir)
      {
         return;
      }

      int[] selRows = _panel.tblBundels.getSelectedRows();
      I18nBundle[] selBundles = _bundlesTableModel.getBundlesForRows(selRows);

      for (int i = 0; i < selBundles.length; i++)
      {
         selBundles[i].writeMissingProps(_app, workDir);
      }
   }

   /**
    * Checks if the workdir is a valid directory name and create the directory
    * if it doesn't exist.
    *
    * @return null if to directory name is not valid
    */
   private File getWorkDir(boolean withMessages)
   {
      String buf = _panel.txtWorkingDir.getText();
      if(null == buf || 0 == buf.trim().length())
      {

         if (withMessages)
         {
            String msg = s_stringMgr.getString("I18n.NoWorkDir");
            // i18n[I18n.NoWorkDir=Please choose a work dir to store your translations.]
            JOptionPane.showMessageDialog(_app.getMainFrame(), msg);
         }
         return null;

      }


      File workDir = new File(buf);
      if(false == workDir.isDirectory())
      {
         if (withMessages)
         {
            String msg = s_stringMgr.getString("I18n.WorkDirIsNotADirectory", workDir.getPath());
            // i18n[I18n.WorkDirIsNotADirectory=Working directory {0} is not a directory]
            JOptionPane.showMessageDialog(_app.getMainFrame(), msg);
         }
         return null;
      }

      if(false == workDir.exists())
      {
         String msg = s_stringMgr.getString("I18n.WorkDirDoesNotExistQuestionCreate", workDir.getPath());
         // i18n[I18n.WorkDirDoesNotExistQuestionCreate=Working directory {0} does not exist.\nDo you want to create it?]

         if (withMessages)
         {
            if(JOptionPane.YES_OPTION ==  JOptionPane.showConfirmDialog(_app.getMainFrame(), msg))
            {
               if(false == workDir.mkdirs())
               {
                  msg = s_stringMgr.getString("I18n.CouldNotCreateWorkDir", workDir.getPath());
                  // i18n[I18n.CouldNotCreateWorkDir=Could not create Working directory {0}]
                  JOptionPane.showMessageDialog(_app.getMainFrame(), msg);
                  return null;

               }
            }
         }
         else
         {
            return null;
         }
      }

      return workDir;

   }


   private void onLoadBundels(IApplication app)
   {
      Locale selLocale = (Locale) _panel.cboLocales.getSelectedItem();


      File workDir = null;
      if(null != _panel.txtWorkingDir.getText() && 0 < _panel.txtWorkingDir.getText().trim().length())
      {
         workDir = new File(_panel.txtWorkingDir.getText());
         if(false == workDir.exists())
         {
            String msg = s_stringMgr.getString("I18n.noWorkdir");
            // i18n[I18n.noWorkdir=Working directory doesn't exist.\nDo you want to create it?]
            if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(app.getMainFrame(), msg))
            {
               workDir.mkdirs();
            }
         }
         else if(false == workDir.isDirectory())
         {
            String msg = s_stringMgr.getString("I18n.WorkdirIsNoDir", workDir.getAbsolutePath());
            // i18n[I18n.WorkdirIsNoDir=The working directory is not a directory.\nNo bundles will be loaded from {0}]
            JOptionPane.showMessageDialog(app.getMainFrame(), msg);

			}
      }
		else
		{
			String msg = s_stringMgr.getString("I18n.noWorkdirSpecified");
			// i18n[I18n.noWorkdirSpecified=No working directory specified.]
			JOptionPane.showMessageDialog(app.getMainFrame(), msg);
		}

		URL[] sourceUrls = getUrlsToLoadI18nPropertiesFrom(workDir);


		String pluginDir = new ApplicationFiles().getPluginsDirectory().getPath();

      ArrayList defaultI18nProps = new ArrayList();
      ArrayList localizedI18nProps = new ArrayList();


      for (int i = 0; i < sourceUrls.length; i++)
      {
         File file = new File(sourceUrls[i].getFile().replaceAll("%20", " "));

         if(file.isDirectory())
         {
            findI18nInDir(selLocale, file, defaultI18nProps, localizedI18nProps, sourceUrls);
         }
         else if (file.getName().equalsIgnoreCase("squirrel-sql.jar") || file.getName().equalsIgnoreCase("fw.jar"))
         {
            findI18nInArchive(selLocale, file, defaultI18nProps, localizedI18nProps, sourceUrls);
         }
         else if(file.getPath().startsWith(pluginDir))
         {
            findI18nInArchive(selLocale, file, defaultI18nProps, localizedI18nProps, sourceUrls);
         }
      }

      Hashtable i18nBundlesByName = new Hashtable();

      for (int i = 0; i < defaultI18nProps.size(); i++)
      {
         I18nProps i18nProps = (I18nProps) defaultI18nProps.get(i);
         I18nBundle pack = new I18nBundle(i18nProps, selLocale, getWorkDir(false), sourceUrls);
         i18nBundlesByName.put(i18nProps.getPath(), pack);
      }

      for (int i = 0; i < localizedI18nProps.size(); i++)
      {
         I18nProps locI18nProps = (I18nProps) localizedI18nProps.get(i);
         String key = locI18nProps.getUnlocalizedPath(selLocale);

         I18nBundle bundle = (I18nBundle) i18nBundlesByName.get(key);
         if(null != bundle)
         {
            bundle.setLocalizedProp(locI18nProps);
         }
      }

      I18nBundle[] bundles = (I18nBundle[]) i18nBundlesByName.values().toArray(new I18nBundle[0]);

      int[] selRows = _panel.tblBundels.getSelectedRows();
      _bundlesTableModel.setBundles(bundles);

      for (int i = 0; i < selRows.length; i++)
      {
         _panel.tblBundels.getSelectionModel().addSelectionInterval(selRows[i], selRows[i]);
      }

   }

	private URL[] getUrlsToLoadI18nPropertiesFrom(File workDir)
	{
		ApplicationFiles af = new ApplicationFiles();

		ArrayList ret = new ArrayList();
		URL[] urls;


		urls = ((URLClassLoader) _app.getClass().getClassLoader()).getURLs();

		for (int i = 0; i < urls.length; i++)
		{
			File file = new File(urls[i].getFile().replaceAll("%20", " "));
			if(file.getName().equals(af.getSQuirrelJarFile().getName()))
			{
				ret.add(urls[i]);
			}
			else if(file.getName().equals(af.getFwJarFile().getName()))
			{
				ret.add(urls[i]);
			}
		}

		PluginInfo[] pi = _app.getPluginManager().getPluginInformation();

		urls = _app.getPluginManager().getPluginURLs();
		for (int i = 0; i < urls.length; i++)
		{
			String jarName = new File(urls[i].getFile()).getName();

			String cleanJarName;
			if(jarName.endsWith(".jar"))
			{
				cleanJarName = jarName.substring(0,jarName.length() - ".jar".length());
			}
			else
			{
				continue;
			}

			for (int j = 0; j < pi.length; j++)
			{
				if(pi[j].getInternalName().equalsIgnoreCase(cleanJarName))
				{
					ret.add(urls[i]);
				}
			}
		}

		return (URL[]) ret.toArray(new URL[ret.size()]);
	}

	private void findI18nInArchive(Locale selLoc, File file, ArrayList defaultI18nProps, ArrayList localizedI18nProps, URL[] sourceUrls)
	{
		try
		{
			ZipFile zf = new ZipFile(file);

			for(Enumeration e=zf.entries(); e.hasMoreElements();)
			{
				ZipEntry entry = (ZipEntry) e.nextElement();


				if(entry.getName().endsWith(".properties"))
				{
					Locale loc = I18nProps.parseLocaleFromPropsFileName(entry.getName());

					if(null == loc)
					{
						defaultI18nProps.add(new I18nProps(file, entry.getName(), sourceUrls));
					}
					if(selLoc.equals(loc))
					{
						localizedI18nProps.add(new I18nProps(file, entry.getName(), sourceUrls));
					}
				}
			}


		}
		catch (IOException e)
		{
			// i18n[I18n.failedToOpenZip=Failed to open zip/jar {0}]
			String msg = s_stringMgr.getString("I18n.failedToOpenZip", file.getAbsolutePath());
			_app.getMessageHandler().showMessage(msg);
			logger.error(msg, e);
		}

	}

   private void findI18nInDir(Locale selLoc, File dir, ArrayList defaultI18nProps, ArrayList localizedI18nProps, URL[] sourceUrls)
   {
      File[] files = dir.listFiles();

      for (int i = 0; i < files.length; i++)
      {
         if(files[i].isDirectory())
         {
            findI18nInDir(selLoc, files[i], defaultI18nProps, localizedI18nProps, sourceUrls);
         }
         else if(files[i].getName().endsWith(".properties"))
         {
            Locale loc = I18nProps.parseLocaleFromPropsFileName(files[i].getName());

            if(null == loc)
            {
               defaultI18nProps.add(new I18nProps(files[i], sourceUrls));
            }
            if(selLoc.equals(loc))
            {
               localizedI18nProps.add(new I18nProps(files[i], sourceUrls));
            }
         }
      }

   }

   public void initialize(IApplication app)
   {
      _app = app;
   }
}
