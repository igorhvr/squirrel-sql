 CREATE DISTINCT TYPE UDTforColumnUDF AS DECIMAL(9,2) 
 WITH COMPARISONS 


 CREATE FUNCTION scalarUDF ( VARCHAR(20) ) 
 RETURNS int 
 EXTERNAL NAME 'UDFsrv!scalarUDF' 
 LANGUAGE java 
 PARAMETER STYLE db2general 
 DETERMINISTIC 
 FENCED 
 NOT NULL CALL 
 NO SQL 
 NO EXTERNAL ACTION 
 NO SCRATCHPAD 
 NO FINAL CALL 
 ALLOW PARALLEL 
 NO DBINFO



 CREATE FUNCTION columnUDF ( UDTforColumnUDF ) 
 RETURNS UDTforColumnUDF 
 SOURCE "SYSIBM".AVG( DECIMAL() ) 