#! /bin/bash

#configure fax service

## Steps


if [[ $EUID -ne 0 ]]; then
     	echo "This script must be run as root" 1>&2
	exit 1
fi

read -p "Mysql database username? " username
read -p "password? " passwd

echo "Changing context.xml using your parameters"

cp context.xml  src/main/webapp/META-INF/
perl -pi -e "s/{user}/$username/"  src/main/webapp/META-INF/context.xml
perl -pi -e "s/{passwd}/$passwd/"  src/main/webapp/META-INF/context.xml

# set db username and password into FlyWay migration script.

read -p "What username do you want to log in as in tomcat? " tomcatUser
read -p "What password do want for $tomcatUser? " tomcatPassword

read -p "Where is tomcat's lib directory? [/usr/share/tomcat6/lib]" libDir

if [ -z "$libDir" ]; then
	libDir="/usr/share/tomcat6/lib"
fi

if [ ! -d "$libDir" ]; then
	read -p "$libDir does NOT exist!  Please enter correct directory " libDir
fi

if [ -d "$libDir" ]; then
	echo "Compiling web service with maven"
	mvn clean
	mvn package
	
#	echo "Copying mysql connector lib to $libDir"
#	cp target/FaxWs-1.0.0-SNAPSHOT/WEB-INF/lib/mysql-connector-java*.jar $libDir
	
	mvn flyway:migrate
	
	echo "Done!"
	echo "You must copy target/FaxWs-1.0.0-SNAPSHOT.war to tomcat's webapps directory"
else
	echo "There is a problem finding $libDir.  Aborting setup.  Please find tomcat's lib directory and rerun this script."
	exit 1;
fi

echo "Updating authentication tables"

mysql -u$username -p$passwd oscarFax -e "insert into users Values('$tomcatUser','$tomcatPassword')"
mysql -u$username -p$passwd oscarFax -e "insert into user_roles Values('$tomcatUser','user')"
