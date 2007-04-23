/*
 * Copyright (C) 2005 Rob Manning
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
package net.sourceforge.squirrel_sql.fw.dialects;

import java.lang.reflect.Field;

import net.sourceforge.squirrel_sql.BaseSQuirreLTestCase;
import net.sourceforge.squirrel_sql.fw.sql.JDBCTypeMapper;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;

public class DialectTestCase extends BaseSQuirreLTestCase {

    protected void testAllTypes(Dialect d) {
        try {
            Field[] fields = java.sql.Types.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                Integer jdbcType = field.getInt(null);
                testType(jdbcType, d);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    protected void testType(int type, Dialect dialect) {
        try {
            dialect.getTypeName(type);
        } catch (MappingException e) {
            System.out.println(
                    "No mapping for type: "+type+"="+
                    JDBCTypeMapper.getJdbcTypeName(type));            
        }
    }

}
