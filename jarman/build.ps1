# clear
lein clean

# building uberjar
lein with-profile client+cli uberjar

# build EXE file 
& 'C:\Program Files (x86)\Launch4j\launch4jc.exe' ..\installer\launch4j.xml

# build udpate package
lein jarman pkg --build

# make `jarman-setup.exe`
& 'c:/Program Files (x86)/Inno Setup 6/ISCC.exe' ..\installer\setup-client.iss


& '..\installer\inno-target\jarman-setup.exe' /SILENT


