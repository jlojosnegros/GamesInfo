# Memoria de la practica de Cloud Computing 



## ToC
<!-- toc -->

- [Estructura elegida para el deployment](#Estructura-elegida-para-el-deployment)
  * [Web Front End](#Web-Front-End)
  * [MailSender](#MailSender)
  * [Base de Datos](#Base-de-Datos)
- [Adaptación de la aplicación al entorno kubernetes](#Adaptacion-de-la-aplicacion-al-entorno-kubernetes)
  * [From Ip to ServiceNames](#From-Ip-to-ServiceNames)
  * [Configuracion Hazelcast](#Configuracion-Hazelcast)
- [Configuraciones adicionales en k8s](#Configuraciones-adicionales-en-k8s)
    + [Hazelcast](#Hazelcast)
    + [Ingress Controller](#Ingress-Controller)
- [Deployment de la aplicacion en minikube](#Deployment-de-la-aplicacion-en-minikube)
  * [Prerrequisitos](#Prerrequisitos)
  * [Creacion de las imagenes](#Creacion-de-las-imagenes)
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

### MailSender
Segun los requisitos:
- este servicio debe desplegarse con **dos** replicas.
  Para ello se tiene un `Deployment`[^deployments] con dos replicas.
  
- NO debe estar expuesto al exterior.
  Para poder exponer las replicas existentes como un solo servicio, sin tener que exponerlo externamente al cluester, se ha optado por un servicio de tipo `ClusterIP`[^service_types]

De nuevo como en el caso anterior, no necesitamos de ningun elemento adicional para realizar balanceo de carga entre las dos instancias levantadas puesto que esta caracteristica la provee k8s.

### Base de Datos
Segun los requisitos la base de datos tenia que utilizar un `Persistent Volume`[^persistent_volumes] en el cluster de k8s para guardar los datos.

## Adaptación de la aplicación al entorno kubernetes
### From Ip to ServiceNames
### Configuracion Hazelcast
## Configuraciones adicionales en k8s
#### Hazelcast
#### Ingress Controller
## Deployment de la aplicacion en minikube
### Prerrequisitos
### Creacion de las imagenes
### Lanzamiento de la aplicacion mediante helm
### Explicacion del fichero de valores `values.yaml`



------------
[^service_types]:https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types
[^deployments]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
[^ingress_controller]: https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/
[^persistent_volumes]: https://kubernetes.io/docs/concepts/storage/persistent-volumes/