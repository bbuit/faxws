Fax Setup for Ubuntu
Fax Web Service
1) Install hylafax and configure it to work with your modem(s)
	root#  apt-get install hylafax-server
Configuring modems is beyond the scope of this document.
2) For each modem and group that receives faxes in OSCAR, create a user on the hylafax server.  It does not need to be on the same server as OSCAR.
	root# adduser –disabled-login –no-create-home teamA
Each user will be the group owner of incoming faxes and the names must match the fax users you create.
3) Create hylafax users using faxadduser.  Remember to name the users the same as the users you created in step 2.
4) Edit FaxDispatch script found in docs.  You need to specify the modem device id(s) and user id(s) you created above in step 2.  If you are using a usb modem you can write a udev rule to always name a usb slot a particular name.  In effect you are creating an alias which hylafax can then use to communicate with the modem.  An example is found is docs. Do not use this example unedited as the subsystem ids will be different on your computer. Copy FaxDispatch to /etc/hylafax.  Copy the udev rule you created to /etc/udev/rules.d
5) Install tomcat6 and Oracle Java 7 on the server which will host the web service.  This does not need to be the same server as OSCAR or hylafax.  Configure tomcat to accept https connections only and use BASIC authentication.  Therefore the connection from OSCAR to the web service is encrypted and requires a user to log in before accessing the service.  Remember the user and password you set up for step 12.
6) Export the public ssl key from OSCAR's tomcat and import it into the java trusted keystore of the web service's installation server.  THIS STEP NEEDS TO BE DONE EVEN IF THEY ALL RUN ON THE SAME SERVER.
7) Clone the project FaxWS from gerrit.  Edit the file faxProperties.properties to suit your setup.  The file is documented. Compile the war file and install in tomcat.
8) In OSCAR's properties file add
-----------------------------------------------------
ModuleNames=Fax 
 #FAX 
faxPollInterval=60000 
consultation_fax_enabled=true 
rx_fax_enabled=true 
consultation_indivica_attachment_enabled=true 
-----------------------------------------------------
Change the poll interval to suit your needs.  The time is in milliseconds.  It specifies the interval between polling the web service for new faxes and sending pending faxes.
9) All faxes are stored in the DOCUMENT_DIR as specified in OSCAR's properties file.
10) In OSCAR's admin interface fill out Clinic name and address.  This is used for the cover page if selected.
11) In OSCAR's admin interface edit the facility and select store signatures in db.  This will preserve the signature for specific consults and prescriptions. If not selected the signature will not display.  Each signature is only valid once.
12) In OSCAR's admin interface, click on Add New Queue.  For each group that receives faxes create a named queue which will be used to access received faxes in the Inbox.
13) In OSCAR's admin interface, assign all privileges to the user(s) via Assign role/Rights to Object for the people who will need access to the newly created queues.
13) In OSCAR's admin interface, select System Management and click on Configure Fax.  Enter the Web Service address as https://192.168.11.100/FaxWs.  Change the ip address to suit your setup.
Enter the Site user and password you created for tomcat in step 5.
For each user you created in hylafax, enter the username(s) and password(s) and select the desired queue in which to receive faxes for that user.
Save your settings.
14) To access the queues you created in step 12, open the inbox and click on Pending Docs at the top.
15) Start hylafax, the FaxWS and restart OSCAR.  You should now have a working fax system in OSCAR.
16) For accounting purposes it is useful if the sender of a fax selects the proper fax line named after the users entered in step 13.  To keep things simple, use the same names for linux user names, hylafax user names and queues.
