@echo off

IF "%JAVA_HOME%"=="" SET LOCAL_JAVA=java
IF NOT "%JAVA_HOME%"=="" SET LOCAL_JAVA=%JAVA_HOME%\bin\java

set basedir=%~f0
:strip
set removed=%basedir:~-1%
set basedir=%basedir:~0,-1%
if NOT "%removed%"=="\" goto strip
set SQUIRREL_SQL_HOME=%basedir%

@rem Check to see if we are running in a 1.6/1.7 JVM and inform the user if not and skip launch. versioncheck.jar 
@rem is a special jar file which has been compiled with javac version 1.2.2, which should be able to be run by 
@rem that version of higher.  The arguments to JavaVersionChecker below specify the minimum acceptable version 
@rem (first arg) and any other acceptable subsequent versions.  <MAJOR>.<MINOR> should be all that is 
@rem necessary for the version form. 
"%LOCAL_JAVA%w" -cp "%SQUIRREL_SQL_HOME%\lib\versioncheck.jar" JavaVersionChecker 1.6 1.7
if ErrorLevel 1 goto ExitForWrongJavaVersion

@rem If the changelist.xml file isn't present or the downloaded update jars don't exist, skip launching the updater - these files are created by the 
@rem software update feature inside of SQuirreL. So their absence, simply means the software update feature hasn't been accessed.
if not exist "%SQUIRREL_SQL_HOME%\update\changeList.xml" goto launchsquirrel
SET TMP_CP="%SQUIRREL_SQL_HOME%\update\downloads\core\squirrel-sql.jar"
if not exist %TMP_CP% goto launchsquirrel
dir /b "%SQUIRREL_SQL_HOME%\update\downloads\core\*.*" > %TEMP%\update-lib.tmp
FOR /F %%I IN (%TEMP%\update-lib.tmp) DO CALL "%SQUIRREL_SQL_HOME%\addpath.bat" "%SQUIRREL_SQL_HOME%\update\downloads\core\%%I"
SET UPDATE_CP=%TMP_CP%
SET UPDATE_PARMS=--log-config-file "%SQUIRREL_SQL_HOME%\update-log4j.properties" --squirrel-home "%SQUIRREL_SQL_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9
"%LOCAL_JAVA%w" -cp %UPDATE_CP% -Dlog4j.defaultInitOverride=true -Dprompt=true net.sourceforge.squirrel_sql.client.update.gui.installer.PreLaunchUpdateApplication %UPDATE_PARAMS%

:launchsquirrel
@rem build SQuirreL's classpath
set TMP_CP="%SQUIRREL_SQL_HOME%\squirrel-sql.jar"
dir /b "%SQUIRREL_SQL_HOME%\lib\*.*" > %TEMP%\squirrel-lib.tmp
FOR /F %%I IN (%TEMP%\squirrel-lib.tmp) DO CALL "%SQUIRREL_SQL_HOME%\addpath.bat" "%SQUIRREL_SQL_HOME%\lib\%%I"
SET SQUIRREL_CP=%TMP_CP%;%CLASSPATH%
echo "SQUIRREL_CP=%SQUIRREL_CP%"

SET TMP_PARMS=--log-config-file "%SQUIRREL_SQL_HOME%\log4j.properties" --squirrel-home "%SQUIRREL_SQL_HOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

@rem Run with a command window.
@rem "%LOCAL_JAVA%" -cp %TMP_CP% net.sourceforge.squirrel_sql.client.Main %TMP_PARMS%

@rem To add translation working directories to your classpath edit and uncomment this line:
@rem start "SQuirreL SQL Client" /B "%LOCAL_JAVA%w" -Xmx256m -cp %TMP_CP%;<your working dir here> net.sourceforge.squirrel_sql.client.Main %TMP_PARMS%

@rem -Dsun.java2d.noddraw=true prevents performance problems on Win32 systems. 

@rem To change the language edit and uncomment this line:
@rem start "SQuirreL SQL Client" /B "%LOCAL_JAVA%w" -Xmx256m -Dsun.java2d.noddraw=true -cp %TMP_CP%;<your working dir here> -Duser.language=<your language here> net.sourceforge.squirrel_sql.client.Main %TMP_PARMS%

@rem Run with no command window. This may not work with older versions of Windows. Use the command above then.
start "SQuirreL SQL Client" /B "%LOCAL_JAVA%w" -Xmx256m -Dsun.java2d.noddraw=true -cp %SQUIRREL_CP% net.sourceforge.squirrel_sql.client.Main %TMP_PARMS%

@rem Run the executable jar file with or without a cmd window. However the
@rem classes from the %CLASSPATH% environment variable will not be available.
@rem "%LOCAL_JAVA%" -jar "%SQUIRREL_SQL_HOME%\squirrel-sql.jar" %TMP_PARMS%
@rem start "SQuirreL SQL Client" /B "%LOCAL_JAVA%w" -Dsun.java2d.noddraw=true -jar "%SQUIRREL_SQL_HOME%\squirrel-sql.jar" %TMP_PARMS%

:ExitForWrongJavaVersion
exit
