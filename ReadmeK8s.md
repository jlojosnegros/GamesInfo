# Memoria de la practica de Cloud Computing 



## ToC
<!-- toc -->

- [Estructura elegida para el deployment](#Estructura-elegida-para-el-deployment)
  * [Web Front End](#Web-Front-End)
    + [Imagen docker](#Imagen-docker)
    + [Deployment](#Deployment)
    + [Service](#Service)
    + [Valores relevantes](#Valores-relevantes)
  * [MailSender](#MailSender)
    + [Imagen docker](#Imagen-docker-1)
    + [Deployment](#Deployment-1)
    + [Servicio](#Servicio)
    + [Valores relevantes](#Valores-relevantes-1)
  * [Base de Datos](#Base-de-Datos)
    + [Creacion del `Persistent Volume`](#Creacion-del-Persistent-Volume)
    + [Nuevo requerimiento](#Nuevo-requerimiento)
    + [Configuracion del chart.](#Configuracion-del-chart)
- [Adaptación de la aplicación al entorno kubernetes](#Adaptacion-de-la-aplicacion-al-entorno-kubernetes)
  * [From Ip to ServiceNames](#From-Ip-to-ServiceNames)
  * [Configuracion Hazelcast](#Configuracion-Hazelcast)
- [Configuraciones adicionales en k8s](#Configuraciones-adicionales-en-k8s)
    + [Hazelcast](#Hazelcast)
    + [Ingress Controller](#Ingress-Controller)
- [Deployment de la aplicacion en minikube](#Deployment-de-la-aplicacion-en-minikube)
  * [Prerrequisitos](#Prerrequisitos)
  * [Creacion de las imagenes](#Creacion-de-las-imagenes)
  * [Configuracion del ingress en minikube](#Configuracion-del-ingress-en-minikube)
  * [Configuracion del hostname](#Configuracion-del-hostname)
  * [Lanzamiento de la aplicacion mediante helm](#Lanzamiento-de-la-aplicacion-mediante-helm)
  * [Explicacion del fichero de valores `values.yaml`](#Explicacion-del-fichero-de-valores-valuesyaml)

<!-- tocstop -->

## Estructura elegida para el deployment
Segun los requisitos del enunciado el despliegue debe realizarse en minikube.

@startuml
actor client
rectangle gamesInfo {
  component [Ingress Controller] as ingress
  component [webFrontEnd] <<ClusterIP>> as webFE {
    component "webFE_01"
    component "webFE_02"
  }
  component [mailSender] <<ClusterIP>> as mailsender {
    component "mailsender_01"
    component "mailsender_02"
  }
  database "MySql" as ddbb{
    database "pv001" <<PersistenVolume>>
  }
}
cloud {
  [internet]
}
client -right-> ingress
ingress -right-> webFE
webFE -down-> ddbb
webFE -right-> mailsender
mailsender -down-> internet

@enduml


### Web Front End
Segun los requisitos:
- El frontal web debe desplegarse con **dos** replicas 
  Para poder tener multiples instancias controladas por k8s se ha dispuesto un `Deployment`[^deployments] con dos replicas.
  
- El frontal debe quedar expuesto al exterior. 
  Para poder exponer al exterior este servicio se deberia haber elegido un servicio k8s de tipo `LoadBalancer`, sin embargo como se va a utilizar un **ingress controller**[^ingress_controller] para poder exponer el servicio al exterior se ha optado por utilizar un servicio de tipo `ClusterIP`.[^service_types]
 
No necesitamos ningun tipo de elemento adicional para balancear la carga entre las dos instancias del frontal web puesto que esta caracteristica nos la ofrece k8s out of the box.

#### Imagen docker
Para poder desplegar el frontal web en kubernetes necesitamos una imagen docker. 
Para construirla a partir del proyecto actual utilizamos el siguiente fichero `Dockerfile`, que puede encontrarse en `./prueba_servidor/Dockerfile`

```Dockerfile
FROM maven:3.6.0-jdk-8-alpine as builder
RUN mkdir /project
COPY ./pom.xml /project/pom.xml
WORKDIR /project
RUN mvn dependency:go-offline
COPY src/ /project/src/
RUN mvn package

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y software-properties-common wget unzip && sudo add-apt-repository ppa:openjdk-r/ppa && sudo apt-get update
RUN sudo apt-get install -y openjdk-8-jre
RUN mkdir /servidor
WORKDIR /servidor
COPY --from=builder /project/target/prueba_servidor-0.0.1-SNAPSHOT.jar /servidor
EXPOSE 8080
CMD java -jar /servidor/prueba_servidor-0.0.1-SNAPSHOT.jar --spring.datasource.url="jdbc:mysql://jlojosnegros-jlojosnegros-mysql:3306/gamesinfo_db?verifyServerCertificate=false&useSSL=true" --spring.datasource.username="root" --spring.datasource.password="gugus" --spring.jpa.hibernate.ddl-auto="update"
```
:warning: el nombre de la url, password, y nombre de la base de datos pasados como parametros tienen que coincidir con los configurados en el fichero de `values.yaml` para el servicio de mysql

Para la construccion de esta imagen docker se ha utilizado:
- Un dockerfile multistage para no tener instaladas en la imagen final herramientas que solo son necesarias en la construccion del proyecto
- Una capa independiente para la descarga de las dependencias del proyecto maven. Esto se realizo en un intento por paliar la "caracteristica" de maven de descargarse medio internet antes de cada compilacion, y ante la imposibilidad de compartir directorios del host en la construccion de una imagen docker. 
  Utilizamos esta capa a modo de "cache" de dependencias, de manera que mientras que no cambiemos el fichero `pom.xml` no tendremos la necesidad de descargarlas de nuevo. 
  ```Dockerfile
  RUN mvn dependency:go-offline
  ```
  
#### Deployment
`./k8s/gamesInfo/templates/webFE_deployment.yaml)`[^webFE_deployment]
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "gamesInfo.fullname" . }}-webfe-deploy
  labels:
    app: {{ .Values.global.appName}}
    tier: {{ .Values.webFE.tier}}
spec:
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: {{ .Values.global.appName}}
      tier: {{ .Values.webFE.tier}}
  replicas: {{ .Values.webFE.replicaCount}} # tells deployment to run 1 pods matching the template
  template: # create pods using pod definition in this template
    metadata:
      labels:
        app: {{ .Values.global.appName}}
        tier: {{ .Values.webFE.tier}}
    spec:
      containers:
      - name: {{ include "gamesInfo.fullname" . }}-webfe-container
        image: "{{ .Values.webFE.image.repository}}:{{ .Values.webFE.image.tag}}"
        imagePullPolicy: {{ .Values.webFE.image.pullPolicy}}
        ports:
        - containerPort: {{ .Values.webFE.port}}
        - containerPort: {{ .Values.webFE.hazelcast.port}}
      initContainers:
      - name: init-mailservice
        image: busybox
        command: ['sh', '-c', 'until nslookup {{ include "gamesInfo.fullname" . }}-mailsender-svc; do echo waiting for {{ include "gamesInfo.fullname" . }}-mailsender-svc; sleep 2; done;']
      - name: init-mysql
        image: busybox
        command: ['sh', '-c', 'until nslookup jlojosnegros-jlojosnegros-mysql; do echo waiting for mysql; sleep 2; done;']
```

#### Service

fichero: `./k8s/gamesInfo/templates/webFE_service.yaml`[^webFE_service]
```yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "gamesInfo.fullname" . }}-webfe-svc
  labels:
    app: {{ .Values.global.appName}}
    tier: {{ .Values.webFE.tier}}
spec:
  ports:
  - name: hazelcast
    port: 5701
  - name: "{{.Values.global.appName}}-webport"
    port: {{ .Values.webFE.port}}
  selector:
    app: {{ .Values.global.appName}}
    tier: {{ .Values.webFE.tier}}
  type: {{ .Values.webFE.serviceType}}
```
#### Valores relevantes 
Aqui podemos ver la seccion relevante del fichero de valores `values.yaml`
```yaml
global:
  appName: jlojosnegros

webFE:
  replicaCount: 1
  tier: front-end
  image:
    repository: prueba_servidor
    tag: latest
    pullPolicy: IfNotPresent
  serviceType: ClusterIP
  port: 8080
  hazelcast:
    port: 5701

```
### MailSender
Segun los requisitos:
- este servicio debe desplegarse con **dos** replicas.
  Para ello se tiene un `Deployment`[^deployments] con dos replicas.
  
- NO debe estar expuesto al exterior.
  Para poder exponer las replicas existentes como un solo servicio, sin tener que exponerlo externamente al cluester, se ha optado por un servicio de tipo `ClusterIP`[^service_types]

De nuevo como en el caso anterior, no necesitamos de ningun elemento adicional para realizar balanceo de carga entre las dos instancias levantadas puesto que esta caracteristica la provee k8s.

#### Imagen docker
Para poder desplegar el frontal web en kubernetes necesitamos una imagen docker. 
Para construirla a partir del proyecto actual utilizamos el siguiente fichero `Dockerfile`, que puede encontrarse en `./mailService/Dockerfile`[^mailService_dockerfile]

```Dockerfile
FROM maven:3.6.0-jdk-8-alpine as builder
RUN mkdir /project
COPY ./pom.xml /project/pom.xml
WORKDIR /project
RUN mvn dependency:go-offline
COPY src/ /project/src/
RUN mvn package

FROM ubuntu:trusty
RUN sudo apt-get update && sudo apt-get install -y software-properties-common wget unzip && sudo add-apt-repository ppa:openjdk-r/ppa && sudo apt-get update 
RUN sudo apt-get install -y openjdk-8-jre
RUN mkdir /mailService
WORKDIR /mailService
COPY --from=builder /project/target/mailService-0.0.1-SNAPSHOT.jar /mailService
EXPOSE 8080
CMD java -jar /mailService/mailService-0.0.1-SNAPSHOT.jar
```
En este dockerfile se han utilizado las mismas tecnicas que se nombran en la construccion del dockerfile del frontal web.

#### Deployment
fichero `./k8s/gamesInfo/templates/mailsender_deployment.yaml`[^mailsender_deployment]
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "gamesInfo.fullname" . }}-mailsender-deploy
  labels:
    app: {{.Values.global.appName}}
    tier: {{.Values.mailSender.tier}}
spec:
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: {{ .Values.global.appName}}
      tier: {{ .Values.mailSender.tier}}
  replicas: {{ .Values.mailSender.replicaCount}}
  template:
    metadata:
      labels:
        app: {{ .Values.global.appName}}
        tier: {{ .Values.mailSender.tier}}
    spec:
      containers:
      - name: {{ include "gamesInfo.fullname" . }}-mailsender-container
        image: "{{ .Values.mailSender.image.repository}}:{{ .Values.mailSender.image.tag}}"
        imagePullPolicy: {{ .Values.mailSender.image.pullPolicy}}
        ports:
        - containerPort: {{ .Values.mailSender.ports.internalPort}}
      initContainers:
      - name: init-mysql
        image: busybox
        command: ['sh', '-c', 'until nslookup jlojosnegros-jlojosnegros-mysql; do echo waiting for mysql; sleep 2; done;']

```

#### Servicio
fichero `./k8s/gamesInfo/templates/mailsender_service.yaml`[^mailsender_service]
```yaml
apiVersion: v1
kind: Service
metadata:
  name: "{{ include "gamesInfo.fullname" . }}-mailsender-svc"
  labels:
    app: {{ .Values.global.appName}}
    tier: {{ .Values.mailSender.tier}}
spec:
  ports:
  - port: {{ .Values.mailSender.ports.internalPort}}
    protocol: TCP
    name: "{{.Values.global.appName}}-mailport"
  selector:
    app: {{ .Values.global.appName}}
    tier: {{ .Values.mailSender.tier}}
  type: {{ .Values.mailSender.serviceType}}
```
#### Valores relevantes
Parametros relevantes dentro del fichero de valores `values.yaml`

```yaml
mailSender:
  replicaCount: 2
  tier: back-end
  image:
    repository: mailservice
    tag: latest
    pullPolicy: IfNotPresent
  serviceType: ClusterIP
  ports:
    # warning: Do NOT change this port unless you also change mailService java app configuration also.
    internalPort: 8080 #port where the mail service is listeninig incoming commands from webFE 
```

### Base de Datos
Segun los requisitos la base de datos tenia que utilizar un `Persistent Volume`[^persistent_volumes] en el cluster de k8s para guardar los datos.

La base de datos utilizada por el proyecto es "MySQl", de modo que utilizaremos un helm chart oficial de mysql para este proposito, y personalizaremos algunos valores utilizando para ellos el fichero `values.yaml`

#### Creacion del `Persistent Volume`
La base de datos utiliza como storage un persisten volume en el cluster de minikube. 
Para ello tenemos que crear el persisten volume como parte del despliegue de nuestra aplicacion.
Este `persistent volume` sera configurado mas adelante en el chart del mysql para ser usado como storage de la base de datos. (#k8s/gamesInfo/templates/pv.yaml)
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv001
  labels:
    type: local
    app: {{ .Values.global.appName}}
spec:
  storageClassName: {{ .Values.pv.storageClass}}
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: {{ .Values.pv.path}}
```
Como vemos algunos de los valores estan parametrizados mediante el fichero `values.yaml`
```yaml
global:
  appName: jlojosnegros
pv:
  storageClass: jlom
  path: "/mnt/data"
```

#### Nuevo requerimiento
Para poder utilizar el char de mysql tendremos que anadirlo como nueva dependencia de nuestro chart.
1. Para ello ponemos las siguientes lineas en el fichero `requirements.yaml` (./k8s/gamesinfo/requirements.yaml)
```yaml
dependencies:
  - name: mysql
    version: < 5.7
    repository: "@stable"
    alias: mysql
```
#### Configuracion del chart.
La aplicacion require de la existencia de un usuario determinado, con una clave determinada, asi como la existencia de una base de datos para poder funcionar. 
Para poder realizar estos pasos, asi como otros necesarios para el correcto funcionamiento de la base de datos en el entorno concreto de la aplicacion, utilizamos los siguientes valores en el fichero `values.yaml`(./k8s/gamesInfo/values.yaml)
```yaml
mysql:
  mysqlRootPassword: gugus
  mysqlDatabase: gamesinfo_db
  persistence:
    enabled: true
    ## database data Persistent Volume Storage Class
    ## If defined, storageClassName: <storageClass>
    ## If set to "-", storageClassName: "", which disables dynamic provisioning
    ## If undefined (the default) or set to null, no storageClassName spec is
    ##   set, choosing the default provisioner.  (gp2 on AWS, standard on
    ##   GKE, AWS & OpenStack)
    ##
    storageClass: jlom
    accessMode: ReadWriteOnce
    size: 8Gi
    annotations: {}
  service:
    type: ClusterIP #yep, it is the default, but just in case.
  configurationFiles:
    mysql.cnf: |-
      [mysqld]
      skip-name-resolve
      bind-address=0.0.0.0
  nameOverride: jlojosnegros-mysql
```
- definimos con `mysqlRootPassword` el password del usuario `root` al valor requerido por la aplicacion. :warning: Este valor debe coincidir con los datos de configuracion pasados al frontal web en la linea de comandos de lanzamiento que se aplica en el fichero [`Dockerfile`](#Imagen-docker)
- definimos con `mysqlDataBase` la creacion de una nueva base de datos con el nombre requerido por la aplicacion.
- definimos el `storageClass` con el mismo valor que en el `persistent volume` que hemos creado para asegurarnos de que el `persistent volume claim`de mysql utiliza el  nuestro y no otro.
- mediante `configurationFiles` nos aseguramos de que la configuracion de red de mysql es la correcta para nuestro entorno.
- mediante `nameOverride` nos aseguramos de que el servicio que expondra la base de datos en el cluster de kubernetes, y que ya hemos configurado como `ClusterIP` para evitar que pueda ser accedida desde fuera del cluster, tenga un nombre fijo al que poder referirnos, puesto que el servicio de frontal web tendra que poder acceder a la base de datos.

2. No aseguramos de tener correctamente configurador el repositorio "@stable" de helm chart. La salida al ejecutar el siguiente comando deberia ser la que se muestra:
```bash
$> helm repo list
NAME    URL                                             
stable  https://kubernetes-charts.storage.googleapis.com
local   http://127.0.0.1:8879/charts 
```
En caso de no ser asi configuramos el repositorio de helm, usando el siguiente comando
```bash
$> helm repo add stable https://kubernetes-charts.storage.googleapis.com/
```
3. Por ultimo actualizamos las dependencias para tener el chart descargado. Este paso puede relegarse hasta el momento de la instalacion si se desea.
```bash
$> helm dependency update
```
## Adaptación de la aplicación al entorno kubernetes
### From Ip to ServiceNames
### Configuracion Hazelcast
## Configuraciones adicionales en k8s
#### Hazelcast
#### Ingress Controller
## Deployment de la aplicacion en minikube
### Prerrequisitos
### Creacion de las imagenes
- eval minikube
- build
### Configuracion del ingress en minikube
- addon enable ingress

### Configuracion del hostname 
/etc/hosts
```bash
echo "$(minikube ip) gamesinfo.example.com" | sudo tee -a /etc/hosts
```
### Lanzamiento de la aplicacion mediante helm
### Explicacion del fichero de valores `values.yaml`



------------
[^service_types]:https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types
[^deployments]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
[^ingress_controller]: https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/
[^persistent_volumes]: https://kubernetes.io/docs/concepts/storage/persistent-volumes/

[^mailService_dockerfile]: https://raw.githubusercontent.com/jlojosnegros/GamesInfo/master/mailService/Dockerfile
[^webFE_deployment]:https://raw.githubusercontent.com/jlojosnegros/GamesInfo/master/k8s/gamesInfo/templates/webFE_deployment.yaml
[^webFE_service]:https://raw.githubusercontent.com/jlojosnegros/GamesInfo/master/k8s/gamesInfo/templates/webFE_service.yaml
[^mailsender_deployment]:https://raw.githubusercontent.com/jlojosnegros/GamesInfo/master/k8s/gamesInfo/templates/mailsender_deployment.yaml
[^mailsender_service]:https://raw.githubusercontent.com/jlojosnegros/GamesInfo/master/k8s/gamesInfo/templates/mailsender_service.yaml