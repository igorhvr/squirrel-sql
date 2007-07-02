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
package net.sourceforge.squirrel_sql.fw.gui;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;

import javax.swing.JComponent;

import net.sourceforge.squirrel_sql.fw.util.FileExtensionFilter;

/**
 * This "describes" the interface of Dialogs which is a static method utility
 * class.  
 * 
 * @author manningr
 *
 */
public interface IDialogUtils {

    File selectFileForWriting(Frame parentFrame,
                              FileExtensionFilter[] filters);
    
    File selectFileForWriting(Frame parentFrame,
                              FileExtensionFilter[] filters, 
                              JComponent accessory);
    
    void showNotYetImplemented(Component owner);
    
    boolean showYesNo(Component owner, String msg);
    
    boolean showYesNo(Component owner, String msg, String title);
    
    void showOk(Component owner, String msg);
}