/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
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
package net.sourceforge.squirrel_sql.client.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * A utility class that can be used to add the ability to drag a file from the 
 * desktop to a session sql editor panel.
 * 
 * @author manningr
 */
public class FileEditorDropTargetListener extends DropTargetAdapter 
                                          implements DropTargetListener {

    /** Logger for this class. */
    private static final ILogger s_log = 
        LoggerController.createLogger(FileEditorDropTargetListener.class);    
    
    /** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(FileEditorDropTargetListener.class); 
    
    private static interface i18n {
        //i18n[FileEditorDropTargetListener.oneFileDropMessage=Only one file 
        //may be dropped onto the editor at a time.]
        String ONE_FILE_DROP_MESSAGE = 
            s_stringMgr.getString("FileEditorDropTargetListener.oneFileDropMessage");
    }
    
    /** the session we are listening for drops into */
    private ISession _session;
    
    public FileEditorDropTargetListener(ISession session) {
        this._session = session;
    }
    
    /**
     * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
     */
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        try {
            DropTargetContext context = dtde.getDropTargetContext();
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            Transferable t = dtde.getTransferable();
            File fileToOpen = null;
            
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                fileToOpen = handleJavaFileList(t);
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                fileToOpen = handleUriListString(t);
            } else {
                s_log.error("drop: flavors fileList and UriListString "
                        + "are not supported");
            }
            if (fileToOpen != null) {
                if (s_log.isInfoEnabled()) {
                    s_log.info("drop: path="+fileToOpen.getAbsolutePath());
                }            
                ISQLPanelAPI api = 
                    _session.getSQLPanelAPIOfActiveSessionWindow(); 
                api.fileOpen(fileToOpen);
            }            
            context.dropComplete(true);
        } catch (Exception e) {
            s_log.error("drop: Unexpected exception "+e.getMessage(),e);
        }

    }

    /**
     * Handles the String data flavor which returns the data as a list of
     * java.io.File objects.
     * 
     * @param t
     *            the transferable to get the list from

     * @return the only file in the list
     * @throws UnsupportedFlavorException
     * @throws IOException
     */    
    private File handleUriListString(Transferable t) 
        throws UnsupportedFlavorException, IOException
    {
        File result = null;
        
        @SuppressWarnings("unchecked")
        String transferData = 
            (String)t.getTransferData(DataFlavor.stringFlavor);
        
        if (transferData != null) {
            // Check to see if the string is a file uri.
            if (transferData.startsWith("file://")) {
                try {
                    // we can have more than one file in the string so tokenize
                    // on whitespace.  Let the user know if we find multiple
                    // tokens that they cannot place drop than one file at a 
                    // time
                    StringTokenizer st = new StringTokenizer(transferData);
                    if (st.countTokens() > 1) {
                        _session.showErrorMessage(i18n.ONE_FILE_DROP_MESSAGE);
                    } else {
                        if (st.hasMoreTokens()) {
                            String fileUrlStr = st.nextToken();
                            URI uri = new URI(fileUrlStr);
                            result = new File(uri);
                        }
                    }
                } catch (URISyntaxException e) {
                    s_log.error("handleUriListString: encountered an "
                            + "invalid URI: " + transferData, e);
                }
            } else {
                // Not a uri - assume it is a string filename.
                result = new File(transferData);
            }
        }
        return result;        
    }
    
    
    /**
     * Handles the JavaFileList data flavor which returns the data as a list of
     * java.io.File objects.
     * 
     * @param t
     *            the transferable to get the list from

     * @return the only file in the list
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    private File handleJavaFileList(Transferable t) 
        throws UnsupportedFlavorException, IOException 
    {
        File result = null;
        
        @SuppressWarnings("unchecked")
        List<File> transferData = 
            (List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
        
        if (transferData.size() > 1) {
            _session.showErrorMessage(i18n.ONE_FILE_DROP_MESSAGE);
        } else {
            result = transferData.get(0);
            if (s_log.isInfoEnabled()) {
                s_log.info("drop: path="+result.getAbsolutePath());
            }
            
        }
        return result;
    }
}
