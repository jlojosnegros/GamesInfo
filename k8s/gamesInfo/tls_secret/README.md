19:30 $ sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout tls.key -out tls.crt
[sudo] password for feanaro: 
Sorry, try again.
[sudo] password for feanaro: 
Can't load /home/feanaro/.rnd into RNG
139759574676544:error:2406F079:random number generator:RAND_load_file:Cannot open file:../crypto/rand/randfile.c:88:Filename=/home/feanaro/.rnd
Generating a RSA private key
.............................................................................................+++++
....+++++
writing new private key to 'tls.key'
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:ES
State or Province Name (full name) [Some-State]:Madrid
Locality Name (eg, city) []:Madrid
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Ericsson
Organizational Unit Name (eg, section) []:CUDB
Common Name (e.g. server FQDN or YOUR name) []:jlojosnegros
Email Address []:jl.ojosnegros.manchon@gmail.com