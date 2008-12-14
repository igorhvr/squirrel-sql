package net.sourceforge.squirrel_sql.client.update;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import net.sourceforge.squirrel_sql.BaseSQuirreLJUnit4TestCase;
import net.sourceforge.squirrel_sql.client.update.UpdateUtil;
import net.sourceforge.squirrel_sql.client.update.xmlbeans.ChannelXmlBean;
import net.sourceforge.squirrel_sql.client.update.xmlbeans.UpdateXmlSerializer;
import net.sourceforge.squirrel_sql.client.update.xmlbeans.UpdateXmlSerializerImpl;
import net.sourceforge.squirrel_sql.fw.util.FileWrapper;
import net.sourceforge.squirrel_sql.fw.util.FileWrapperFactory;
import net.sourceforge.squirrel_sql.fw.util.FileWrapperFactoryImpl;
import net.sourceforge.squirrel_sql.fw.util.IProxySettings;
import net.sourceforge.squirrel_sql.fw.util.ProxySettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class UpdateUtilExternalTest extends BaseSQuirreLJUnit4TestCase {

    UpdateUtil utilUnderTest = new UpdateUtilImpl();
    UpdateXmlSerializer serializer = null;
    FileWrapperFactory fileWrapperFactory = new FileWrapperFactoryImpl(); 
    IProxySettings proxySettings = new ProxySettings();
    
    @Before
    public void setUp() throws Exception {
        //utilUnderTest = ;
        serializer = new UpdateXmlSerializerImpl();
    }

    @After
    public void tearDown() {
        //utilUnderTest = null;
    }
    
    @Test
    public void testFileDownload() throws Exception {
        String host = "squirrel-sql.sourceforge.net";
        String file = "firebird_object_tree.jpg";
        //String path = "/downloads/";
        int port = 80;
        //UpdateUtil util = new UpdateUtilImpl();
        utilUnderTest.downloadHttpUpdateFile(host, port, file, "/tmp", -1, -1, proxySettings);
        verifyFileExistsAndDeleteIt("firebird_object_tree.jpg", false);
    }
    
    @Test
    public void downloadHttpFile() throws Exception {
        String host = "squirrel-sql.sourceforge.net";
        String file = UpdateUtil.RELEASE_XML_FILENAME;
        //String path = "/release/snapshot/";
        int port = 80;
        utilUnderTest.downloadHttpUpdateFile(host, port, file, "/tmp", -1, -1, proxySettings);
        verifyFileExistsAndDeleteIt(file, false);
    }

    @Test
    public void testExtractZipFile() throws Exception {
   	 FileWrapper graphZip = fileWrapperFactory.create("/tmp/graph.zip");
   	 FileWrapper extractDir = fileWrapperFactory.create("/tmp/extract");
   	 utilUnderTest.extractZipFile(graphZip, extractDir);
    }
    
    /**
     * Tests the downloadCurrentRelease method by downloading the file: 
     * 
     * http://squirrel-sql.sourceforge.net/release/snapshot/release.xml
     * 
     * @throws Exception
     */
    @Test
    public void testFileDownloadCurrentRelease() throws Exception {
        String host = "http://squirrel-sql.sourceforge.net";
        String path = "/release/snapshot/";
        String file = UpdateUtil.RELEASE_XML_FILENAME;
        
        ChannelXmlBean bean = utilUnderTest.downloadCurrentRelease(host, 80, path, file, proxySettings);
        assertNotNull(bean);
        serializer.write(bean, "/tmp/test.xml");
        verifyFileExistsAndDeleteIt("test.xml", false);
    }
    
    
    
    private void verifyFileExistsAndDeleteIt(String filename, boolean delete) throws Exception {
       File downloadFile = new File("/tmp", filename);
       assertTrue(downloadFile.exists());
       if (delete) {
      	 downloadFile.delete();
       }
    }
    
}
