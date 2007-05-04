package net.sourceforge.squirrel_sql.fw.sql;
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
public interface GenericSQL {
    
    public final static String CREATE_STUDENT = 
        "create table student ( " +
        "    sno     integer, " +
        "    sname   varchar(10), " +
        "    age     integer " +
        "); ";
    
    public final static String CREATE_COURSES = 
        "create table courses ( " +
        "    cno     varchar(5), " +
        "    title   varchar(10), " +
        "    credits integer " +
        "); ";            
    
    public final static String CREATE_PROFESSOR = 
        "create table professor ( " +
        "    lname   varchar(10), " +
        "    dept    varchar(10), " +
        "    salary  integer, " +
        "    age     integer " +
        "); ";
        
    public final static String CREATE_TAKE = 
        "create table take ( " +
        "    sno     integer, " +
        "    cno varchar(15) " +
        "); ";
    
    
    public final static String CREATE_TEACH = 
        "create table teach ( " +
        "    lname   varchar(10), " +
        "    cno     varchar(5) " +
        "); ";

    public final static String STUDENTS_NOT_TAKING_CS112 = 
        "select * " +
        "from student " +
        "where sno not in (select sno " +
        "                  from take " +
        "                  where cno = 'CS112'); ";       
    
    public final static String ANSI_SQL_92_PROCEDURE =
        "CREATE PROCEDURE TRANSFER (:FROM CHAR(7),:TO CHAR(7),:AMOUNT DECIMAL(7,2)) BEGIN /* remove the amount from FROM account */ UPDATE ACCOUNTS SET BALANCE = BALANCE - :AMOUNT WHERE ACCOUNT_ID = :FROM ; /* add the amount to TO account */ UPDATE ACCOUNTS SET BALANCE = BALANCE + AMOUNT WHERE ACCOUNT_ID = :TO ; END; ";
    
    public final static String ANSI_SQL_92_PROCEDURE_READABLE =
        "CREATE PROCEDURE TRANSFER ( :FROM    CHAR(7), " +
        "                            :TO      CHAR(7), " +
        "                            :AMOUNT  DECIMAL(7,2) ) " +
        "    BEGIN " +
        "        /* remove the amount from FROM account */ " +
        "        UPDATE ACCOUNTS " +
        "        SET BALANCE = BALANCE - :AMOUNT " +
        "        WHERE ACCOUNT_ID = :FROM ; " +
        " " +
        "        /* add the amount to TO account */ " +
        "        UPDATE ACCOUNTS " +
        "        SET BALANCE = BALANCE + AMOUNT " +
        "        WHERE ACCOUNT_ID = :TO ; " +
        "    END; ";        
}
