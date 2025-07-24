# fps-bus-ms-venar-ravash

> sistema control inventario IA

El proyecto usa Java 21, Maven y Springboot 3.2.2

---
## Estructura de carpetas del proyecto

* **Codigo Java** : consta del `pom.xml` y la carpeta `src`
* **Documentacion del api** : la documentación de puede revisar desde la ruta de swagger.ui
* **Docker** : consta de los archivos `Dockerfile` y `.dockerignore`
* **GKE** : consta de los archivos `gke-pipeline.yml` y `gke-deployment`.yml la carpeta `k8s/gke`
* **SonarCloud** : es una herramienta para la calidad del codigo, en el archivo `sonar-project.properties` esta la configuracion
* **Newrelic** : dentro de la carpeta `newrelic` se encuentra la configuración para el uso de la herramienta para el registro de logs

---

## Despliegues

* JDK y Maven
* Docker
* GKE / Azure Pipelines

---

## Pruebas

Pre requisitos

1. En el archivo `application.yml` cambiar el profile active a DEV o LOCAL o como variable de entorno dentro del IDE que se esté utilizando.

Ir al siguiente link: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) para recivir un `200` con el mensaje `It's running`.

--- 

## Seguridad

El proyecto se generar con un parámetro a enviar a través del header `app-id` cuyo valor de encuentra el el archivo `application.yml` el cual es necesario para realizar las pruebas hacia los endpoints.

Si se utiliza otro método de seguridad y/o autenticación se puede omitir esta configuración que se encuentra en la siguiente ruta `/infrastructure/config/exception/TransactionFilter.java`

---

## Configuración

En la ruta `src/main/resources/` se encuentran los archivos de configuración de spring y la aplicacion que son la configuracion predeterminada, estas variables se pueden configurar fuera del archivo usando variables de ambiente o variables del server donde esta corriendo.

En la ruta `src/main/resources/gcp` se encuentra los service accounts para las conexiones y comunicación con los servicios de GCP. 


---
**Es importante mantener esta información privada y para fines únicamente del desarrollo de proyectos de Farmacias Peruanas**
