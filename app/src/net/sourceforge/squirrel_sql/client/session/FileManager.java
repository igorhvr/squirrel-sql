package net.sourceforge.squirrel_sql.client.session;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.fw.gui.ChooserPreviewer;
import net.sourceforge.squirrel_sql.fw.gui.Dialogs;
import net.sourceforge.squirrel_sql.fw.util.FileExtensionFilter;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;


public class FileManager
{
   private ISQLPanelAPI _sqlPanelAPI;

   private File _toSaveTo = null;

   private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(FileManager.class);
   
   private JFileChooser fileChooser = null;
   
   FileManager(ISQLPanelAPI sqlPanelAPI)
   {
      _sqlPanelAPI = sqlPanelAPI;
   }

   public boolean save()
   {
      return saveIntern(false);
   }

   public boolean saveAs()
   {
      return saveIntern(true);
   }
      
   public boolean open(boolean appendToExisting)
   {
       boolean result = false;
      JFileChooser chooser = getFileChooser();
      chooser.addChoosableFileFilter(new FileExtensionFilter("Text files", new String[]{".txt"}));
      chooser.addChoosableFileFilter(new FileExtensionFilter("SQL files", new String[]{".sql"}));
      chooser.setAccessory(new ChooserPreviewer());

      SquirrelPreferences prefs = _sqlPanelAPI.getSession().getApplication().getSquirrelPreferences();
      MainFrame frame = _sqlPanelAPI.getSession().getApplication().getMainFrame();


      if (prefs.isFileOpenInPreviousDir())
      {
         String fileName = prefs.getFilePreviousDir();
         if (fileName != null)
         {
            chooser.setCurrentDirectory(new File(fileName));
         }
      }
      else
      {
         String dirName = prefs.getFileSpecifiedDir();
         if (dirName != null)
         {
            chooser.setCurrentDirectory(new File(dirName));
         }
      }
      _sqlPanelAPI.getSession().selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);
      if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
      {
          result = true;
         File selectedFile = chooser.getSelectedFile();
         if (!appendToExisting) {
             _sqlPanelAPI.setEntireSQLScript("");
         }
         loadScript(selectedFile);
         prefs.setFilePreviousDir(selectedFile.getAbsolutePath());
      }
      return result;
   }

   private void loadScript(File file)
   {
      FileInputStream fis = null;
      BufferedInputStream bis = null;
      try
      {
         StringBuffer sb = new StringBuffer();
         fis = new FileInputStream(file);
         bis = new BufferedInputStream(fis);
         byte[] bytes = new byte[2048];
         int iRead = bis.read(bytes);
         while (iRead != -1)
         {
            sb.append(new String(bytes, 0, iRead));
            iRead = bis.read(bytes);
         }
         _sqlPanelAPI.appendSQLScript(sb.toString(), true);
         setFile(file);
      }
      catch (java.io.IOException io)
      {
         _sqlPanelAPI.getSession().getMessageHandler().showErrorMessage(io);
      }
      finally
      {
          if (bis != null)
          {
              try { bis.close(); } catch (IOException ignore) {}
          }
          if (fis != null) 
          {
              try { fis.close(); } catch (IOException ignore) {}
          }
      }
   }


   public boolean saveIntern(boolean toNewFile)
   {
       boolean result = false;
      if (toNewFile)
      {
         _toSaveTo = null;
      }


      JFileChooser chooser = getFileChooser();

      HashMap fileAppenixes = new HashMap();
      FileExtensionFilter filter;
      filter = new FileExtensionFilter("Text files", new String[]{".txt"});
      chooser.addChoosableFileFilter(filter);
      fileAppenixes.put(filter, ".txt");

      filter = new FileExtensionFilter("SQL files", new String[]{".sql"});
      chooser.addChoosableFileFilter(filter);
      fileAppenixes.put(filter, ".sql");

      SquirrelPreferences prefs = _sqlPanelAPI.getSession().getApplication().getSquirrelPreferences();
      MainFrame frame = _sqlPanelAPI.getSession().getApplication().getMainFrame();

      for (; ;)
      {
         if (null == _toSaveTo)
         {
            if (prefs.isFileOpenInPreviousDir())
            {
               String dirName = prefs.getFilePreviousDir();
               if (dirName != null)
               {
                  chooser.setCurrentDirectory(new File(dirName));
               }
            }
            else
            {
               String dirName = prefs.getFileSpecifiedDir();
               if (dirName != null)
               {
                  chooser.setCurrentDirectory(new File(dirName));
               }
            }
         }

         _sqlPanelAPI.getSession().selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);

         if (null != _toSaveTo)
         {
             if (saveScript(frame, _toSaveTo, false)) {
                 result = true;
             }
            break;
         }

         if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION)
         {
             
            _toSaveTo = chooser.getSelectedFile();

            if (!_toSaveTo.exists() && null != fileAppenixes.get(chooser.getFileFilter()))
            {
               if (!_toSaveTo.getAbsolutePath().endsWith(fileAppenixes.get(chooser.getFileFilter()).toString()))
               {
                  _toSaveTo = new File(_toSaveTo.getAbsolutePath() + fileAppenixes.get(chooser.getFileFilter()));
               }
            }

            if (saveScript(frame, _toSaveTo, true))
            {
               result = true;
               break;
            } else {
                result = false;
                break;
            }
         }
         else
         {
            break;
         }
      }
      return result;
   }

   private boolean saveScript(JFrame frame, File file, boolean askReplace)
   {
      boolean doSave = false;
      if (askReplace && file.exists())
      {
          // i18n[FileManager.confirm.filereplace={0} \nalready exists. Do you want to replace it?]
         String confirmMsg = 
             s_stringMgr.getString("FileManager.confirm.filereplace", 
                                   file.getAbsolutePath());
          doSave =
            Dialogs.showYesNo(frame, confirmMsg);
         //i18n
         if (!doSave)
         {
            return false;
         }
         if (!file.canWrite())
         {
             // i18n[FileManager.error.cannotwritefile=File {0} \ncannot be written to.]
             String msg = 
                 s_stringMgr.getString("FileManager.error.cannotwritefile", 
                                       file.getAbsolutePath());
            Dialogs.showOk(frame, msg);
            return false;
         }
         file.delete();
      }
      else
      {
         doSave = true;
      }


      SquirrelPreferences prefs = _sqlPanelAPI.getSession().getApplication().getSquirrelPreferences();

      if (doSave)
      {
         prefs.setFilePreviousDir(file.getParent());

         FileOutputStream fos = null;
         try
         {
            fos = new FileOutputStream(file);

            String sScript = _sqlPanelAPI.getEntireSQLScript();

            fos.write(sScript.getBytes());
            setFile(file);
            // i18n[FileManager.savedfile=Saved to {0}]
            String msg = s_stringMgr.getString("FileManager.savedfile",
                                               file.getAbsolutePath());
            _sqlPanelAPI.getSession().getMessageHandler().showMessage(msg);
         }
         catch (IOException ex)
         {
            _sqlPanelAPI.getSession().getMessageHandler().showErrorMessage(ex);
         }
         finally
         {
            if (fos != null)
            {
               try
               {
                  fos.close();
               }
               catch (IOException ignore)
               {
               }
            }
         }
      }
      return true;
   }

   private void setFile(File file)
   {
      _toSaveTo = file;
      _sqlPanelAPI.getSession().getActiveSessionWindow().setSqlFile(file);
   }

   private JFileChooser getFileChooser() {
       if (fileChooser == null) {
           fileChooser = new JFileChooser();
       }
       return fileChooser;
   }
   
   public void clearCurrentFile()
   {
      _toSaveTo = null;
   }
   
}
