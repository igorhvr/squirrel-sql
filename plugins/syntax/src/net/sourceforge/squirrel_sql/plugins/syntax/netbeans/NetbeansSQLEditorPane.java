package net.sourceforge.squirrel_sql.plugins.syntax.netbeans;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.parser.kernel.ErrorInfo;
import net.sourceforge.squirrel_sql.client.session.parser.ParserEventsAdapter;
import net.sourceforge.squirrel_sql.plugins.syntax.SyntaxPreferences;
import net.sourceforge.squirrel_sql.plugins.syntax.KeyManager;
import net.sourceforge.squirrel_sql.plugins.syntax.SyntaxPugin;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;

import org.netbeans.editor.*;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.*;


public class NetbeansSQLEditorPane extends JEditorPane
{
   private boolean _parsingInitialized;
   private ISession _session;
   private ErrorInfo[] _currentErrorInfos = new ErrorInfo[0];
   private SyntaxPreferences _prefs;
   private SyntaxFactory _syntaxFactory;
   private SyntaxPugin _plugin;

   private IIdentifier _sqlEntryPanelIdentifier;

   public NetbeansSQLEditorPane(ISession session, SyntaxPreferences prefs, SyntaxFactory syntaxFactory, SyntaxPugin plugin, IIdentifier sqlEntryPanelIdentifier)
   {
      _session = session;
      _prefs = prefs;
      _syntaxFactory = syntaxFactory;
      _plugin = plugin;
      _sqlEntryPanelIdentifier = sqlEntryPanelIdentifier;

      _syntaxFactory.putEditorPane(_session, this);

      Settings.removeInitializer(BaseSettingsInitializer.NAME);
      Settings.addInitializer(new BaseSettingsInitializer(), Settings.CORE_LEVEL);
      /////////////////////////////////////////////////////////////////////////////////
      // There are a lot of goodies in the ExtSettingsInitializer
      // that might be interesting in th future.
      // Unfortunately some conflicts with some of Squirrels shortcuts
      // are in ExtSettingsInitializer
      //Settings.removeInitializer(ExtSettingsInitializer.NAME);
      //Settings.addInitializer(new ExtSettingsInitializer(), Settings.CORE_LEVEL);
      //
      /////////////////////////////////////////////////////////////////////////////
      Settings.removeInitializer(SQLSettingsInitializer.NAME);

      Font font = _session.getProperties().getFontInfo().createFont();
      Settings.addInitializer(new SQLSettingsInitializer(SQLKit.class, _prefs, font, _plugin));


      //////////////////////////////////////////////////////////////////////////////////////////
      //Needs to be done at this moment. That's why we can't call updateFromPreferences() here.
      setEditorKit(new SQLKit(syntaxFactory));
      //
      //////////////////////////////////////////////////////////////////////////////////////////

      modifyKeyStrokes();

      Document doc = getDocument();
      _syntaxFactory.putDocument(_session, doc);


      setToolTipText("Just to make getToolTiptext() to be called");

      new KeyManager(this);
   }

   private void modifyKeyStrokes()
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // The ctrl enter short cut in the Netbeans editor is set in org.netbeans.editor.ext.BaseKit
      // to the org.netbeans.editor.ext.BaseKit.SplitLineAction.
      // Since the ctrl enter shorcut is a basic SQuirreL short cut and is defined via the main menu action
      // we must remove this binding here.
      KeyStroke ctrlEnterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlEnterStroke);

      // Removed for the SQLBookmark Plugin
      KeyStroke ctrlJStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlJStroke);

      // Removed for the tools popup
      KeyStroke ctrlTStroke = KeyStroke.getKeyStroke(KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlTStroke);

      // Removed for reformatting
      KeyStroke ctrlShiftFStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK | java.awt.event.InputEvent.SHIFT_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlShiftFStroke);

      // Removed for duplicate line
      KeyStroke ctrlDStroke = KeyStroke.getKeyStroke(KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlDStroke);

      // Removed for comment
      KeyStroke ctrlSubstractStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.CTRL_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlSubstractStroke);

      // Removed for uncomment
      KeyStroke ctrlShiftSubstractStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.CTRL_MASK | java.awt.event.InputEvent.SHIFT_MASK);
      getKeymap().removeKeyStrokeBinding(ctrlShiftSubstractStroke);

		// Removed for cut SQL
		KeyStroke ctrlShiftXStroke = KeyStroke.getKeyStroke(KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK | java.awt.event.InputEvent.SHIFT_MASK);
		getKeymap().removeKeyStrokeBinding(ctrlShiftXStroke);

		// Removed for copy SQL
		KeyStroke ctrlShiftCStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK | java.awt.event.InputEvent.SHIFT_MASK);
		getKeymap().removeKeyStrokeBinding(ctrlShiftCStroke);

		//
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////


      ////////////////////////////////////////////////////////////
      // The parser didn't get triggered on shift+insert.
      // We do it here by hand
      KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, Event.SHIFT_MASK);
      final Action origAction = getKeymap().getAction(ks);
      Action triggerParserAction = new AbstractAction()
      {
         public void actionPerformed(ActionEvent e)
         {
            // This event does not always hit the righr editor !?
            // That's why we can't use _sqlEntryPanelIdentifier
            origAction.actionPerformed(e);
            if(_session.getActiveSessionWindow().hasSQLPanelAPI())
            {
               IIdentifier entryPanelId = _session.getSQLPanelAPIOfActiveSessionWindow().getSQLEntryPanel().getIdentifier();
               _session.getParserEventsProcessor(entryPanelId).triggerParser();
            }
         }
      };
      //
      //////////////////////////////////////////////////////////

      getKeymap().addActionForKeyStroke(ks, triggerParserAction);
   }

   public void updateFromPreferences()
   {
      Settings.removeInitializer(BaseSettingsInitializer.NAME);
      Settings.addInitializer(new BaseSettingsInitializer(), Settings.CORE_LEVEL);
      /////////////////////////////////////////////////////////////////////////////////
      // There are a lot of goodies in the ExtSettingsInitializer
      // that might be interesting in th future.
      // Unfortunately some conflicts with some of Squirrels shortcuts
      // are in ExtSettingsInitializer
      //Settings.removeInitializer(ExtSettingsInitializer.NAME);
      //Settings.addInitializer(new ExtSettingsInitializer(), Settings.CORE_LEVEL);
      //
      /////////////////////////////////////////////////////////////////////////////
      Settings.removeInitializer(SQLSettingsInitializer.NAME);

      Font font = _session.getProperties().getFontInfo().createFont();
      Settings.addInitializer(new SQLSettingsInitializer(SQLKit.class, _prefs, font, _plugin));


      modifyKeyStrokes();

      Document doc = getDocument();
      _syntaxFactory.putDocument(_session, doc);

   }


   public String getToolTipText(MouseEvent event)
   {
      int pos = viewToModel(event.getPoint());

      initParsing();

      for (int i = 0; i < _currentErrorInfos.length; i++)
      {
         if(_currentErrorInfos[i].beginPos-1 <= pos && pos <= _currentErrorInfos[i].endPos)
         {
            return _currentErrorInfos[i].message;
         }
      }

      return null;
   }


   private void initParsing()
   {
      if(false == _parsingInitialized && null != _session.getParserEventsProcessor(_sqlEntryPanelIdentifier))
      {
         _parsingInitialized = true;
         _session.getParserEventsProcessor(_sqlEntryPanelIdentifier).addParserEventsListener(new ParserEventsAdapter()
         {
            public void errorsFound(ErrorInfo[] errorInfos)
            {
               onErrorsFound(errorInfos);
            }
         });
      }
   }

   private void onErrorsFound(ErrorInfo[] errorInfos)
   {
      _currentErrorInfos = errorInfos;
   }

   public String getText()
   {
      return super.getText().replaceAll("\r\n", "\n");
   }

   public void setUndoManager(UndoableEditListener um)
   {
      getDocument().addUndoableEditListener(um);
      getDocument().putProperty( BaseDocument.UNDO_MANAGER_PROP, um );
   }

   public IIdentifier getSqlEntryPanelIdentifier()
   {
      return _sqlEntryPanelIdentifier;
   }

}
