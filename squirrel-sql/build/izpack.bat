pushd \apps\izpack\bin
set LCL_SQL=\src\squirrel-sql
d:\apps\jdk13\bin\java -jar "../lib/compiler.jar" %LCL_SQL%\build\izpack-complete.xml -b %LCL_SQL% -k standard-kunststoff -o %LCL_SQL%\dist\squirrel-sql-1.1alpha1\squirrel-sql-1.1alpha1-install-complete.jar
d:\apps\jdk13\bin\java -jar "../lib/compiler.jar" %LCL_SQL%\build\izpack-basic.xml -b %LCL_SQL% -k standard-kunststoff -o %LCL_SQL%\dist\squirrel-sql-1.1alpha1\squirrel-sql-1.1alpha1-install-basic.jar
popd
