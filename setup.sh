#! /bin/bash

#configure fax service

echo "CAUTION! This script will restart Tomcat after execution."

# 0. check for privilege
if [[ $EUID -ne 0 ]]; then
     	echo "This script must be run as root" 1>&2
	exit 1
fi

# if not already downloaded.

## 1. git
#sudo apt install git
#
## 2. maven
#sudo apt install maven
#
## 3. Clone
#git clone https://dennis_warren@bitbucket.org/openoscar/faxws.git
#
## 4. Checkout
## When release is avaialable

# 5. current MySQL pass and username
read -p "MySQL database username? " username
read -p "MySQL database password? " passwd

echo "Configuring Tomcat"

perl -pi -e "s/{user}/$username/"  src/main/webapp/META-INF/context.xml
perl -pi -e "s/{passwd}/$passwd/"  src/main/webapp/META-INF/context.xml

# 6. new FaxWs pass and username
read -p "What username do you want to log in as in tomcat? " tomcatUser
read -p "What password do want for $tomcatUser? " tomcatPassword

# 7. build FaxWs mvn clean package
echo "Compiling web service with maven"
mvn clean
mvn package

# 8. Migrate mvn flyway:migrate
echo "Migrating database"
mvn flyway:migrate -Dflyway.user=$username -Dflyway.password=$passwd

# 9. Add pass and username to db
echo "Updating authentication tables"
mysql -u$username -p$passwd oscarFax -e "insert into users Values('$tomcatUser','$tomcatPassword')"
mysql -u$username -p$passwd oscarFax -e "insert into user_roles Values('$tomcatUser','user')"

# 10. Move WAR and JDBC Driver to Tomcat

# a. find the highest version Tomcat running on this server
TOMCAT=$(ps aux | grep org.apache.catalina.startup.Bootstrap | grep -v grep | awk '{ print $1 }')
if [ -z "$TOMCAT" ]; then
    #Tomcat is not running, find the highest installed version
    if [ -f /usr/share/tomcat9/bin/version.sh ] ; then
    TOMCAT=tomcat9
    else
         if [ -f /usr/share/tomcat8/bin/version.sh ] ; then
         TOMCAT=tomcat8
         else
              if [ -f /usr/share/tomcat7/bin/version.sh ] ; then
              TOMCAT=tomcat7
             fi
         fi
    fi
fi

# b. Confirm that the default CATALINA_BASE path is correct. If not correct, then try to build it.   
if [ ! -d "${TOMCAT_PATH}" ];
	then
		TOMCAT_PATH=\/var\/lib\/${TOMCAT}\/webapps
fi

if [ ! -d "${TOMCAT_PATH}" ];
	then
		while read -p "Error: Catalina Base not defined. Enter path for Tomcat webapps directory (ie. /var/lib/tomcat6/webapps): " TOMCAT_BASE
		do	
			if [ -d "${TOMCAT_BASE}" ];
				then
					TOMCAT_PATH=${TOMCAT_BASE}
					break
				else
					echo " ERROR: Tomcat webapps not found at: ${TOMCAT_BASE}"
			fi
		done					
fi

# c. move the WAR file. 
if [ -d "${TOMCAT_PATH}" ];
	sudo mv -f ./target/FaxWs*SNAPSHOT ${TOMCAT_PATH}/faxWs.war
fi

# d. move the JDBC Jar
if [ -d "/usr/share/${TOMCAT}/lib" ];
	cp -f ./target/FaxWs-1.0.0-SNAPSHOT/WEB-INF/lib/mysql-connector-java*.jar /usr/share/${TOMCAT}/lib/
fi

# 11. Restart Tomcat
# sudo service ${TOMCAT} restart
