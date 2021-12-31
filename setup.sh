#! /bin/bash

#configure fax service

# 0. check for privilege
if [[ $EUID -ne 0 ]]; then
     	echo "This script must be run as root" 1>&2
	exit 1
fi

## 1. git
sudo apt install git

## 2. maven
sudo apt install maven

## 3. Clone
mkdir -p ./git
cd ./git
git clone https://dennis_warren@bitbucket.org/openoscar/faxws.git
cd ./faxws

## 4. Checkout
#git checkout release

# 5. current MySQL pass and username
echo "Setting passwords... "

read -p "MySQL database username? " username
read -p "MySQL database password? " passwd

perl -pi -e "s/{user}/$username/"  src/main/webapp/META-INF/context.xml
perl -pi -e "s/{passwd}/$passwd/"  src/main/webapp/META-INF/context.xml

# 6. new FaxWs pass and username
read -p "What username do you want to log in as in tomcat? " tomcatUser
read -p "What password do want for $tomcatUser? " tomcatPassword

# 7. build FaxWs mvn clean package
echo "Build FaxWs with Maven..."

mvn clean
mvn package

# 8. Migrate mvn flyway:migrate
## cannot use on older MySQL databases. Must be 5.7 and up or this command will fail.
mvn flyway:migrate -Dflyway.user=$username -Dflyway.password=$passwd

# 9. Add pass and username to db
echo "Setting authentication database..."

mysql -u$username -p$passwd oscarFax -e "insert into users Values('$tomcatUser','$tomcatPassword')"
mysql -u$username -p$passwd oscarFax -e "insert into user_roles Values('$tomcatUser','user')"

# 10. Move WAR and JDBC Driver to Tomcat
echo "Installing FaxWs..."

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
	then
		mv -f ./target/FaxWs*SNAPSHOT.war ${TOMCAT_PATH}/faxWs.war
fi

# d. move the JDBC Jar
if [ -d "/usr/share/${TOMCAT}/lib" ];
	then
		cp ./target/FaxWs-1.0.0-SNAPSHOT/WEB-INF/lib/mysql-connector-java*.jar /usr/share/${TOMCAT}/lib/
fi
