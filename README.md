# PARNAUGO - Proyecto VelesHack

⚠️ **Estado del Proyecto:** Esta es una versión muy preliminar (prueba de concepto / MVP), pero la arquitectura base ya se encuentra completamente funcional y operativa.

## 🌐 Entornos y Enlaces de Acceso

Los servicios principales están desplegados y son accesibles en las siguientes direcciones para realizar las pruebas:

- **Interfaz Web (UI) / API:** [http://arnauquest.com:5555](http://arnauquest.com:5555)
- **Broker MQTT:** `arnauquest.com` (Puerto: `1883`, **Sin SSL/TLS**)

## 👨‍💻 Autores

Este proyecto ha sido desarrollado por:
- **Arnau García Taberner**
- **Paula García Martiniez**
- **Rodrigo Losa Bessia**

---

## 🏗️ Estructura del Repositorio

Este directorio contiene todo el código fuente y las configuraciones de infraestructura necesarias para levantar el ecosistema completo del proyecto:

### 1. Aplicación Móvil Android (`/app`)
Aplicación cliente desarrollada en Kotlin usando la arquitectura moderna Jetpack Compose. Sus características principales son:
- **Visualizador Web:** Integra un WebView para acceder directamente a la Interfaz Web.
- **Servicio MQTT en Background:** Cuenta con un servicio nativo en segundo plano (*Foreground Service*) que mantiene una conexión constante con el broker MQTT. Se suscribe a los topics que decida el usuario y lanza **notificaciones push** en el móvil en tiempo real cuando hay nuevos mensajes o actualizaciones.

### 2. API y UI (`/iota-messages-api`)
Backend desarrollado en Python que expone tanto la interfaz web interactiva como la lógica de negocio necesaria para enviar y leer datos.

### 3. Red Privada IOTA Tangle (`/iota-tangle`)
Archivos de configuración y manifiestos para desplegar una red DLT (Distributed Ledger Technology) privada basada en IOTA. Incluye:
- **IOTA Hornet:** El nodo principal de la red.
- **INX Coordinator:** El coordinador que emite los hitos (milestones) de la red privada.
- **INX Dashboard:** Panel de control visual de la red.

### 4. Broker de Mensajería (`/mosquitto`)
Configuración base para el broker MQTT (Eclipse Mosquitto) que orquesta la comunicación de eventos en tiempo real entre el backend y la app móvil.

### 5. Despliegue Automatizado
En la raíz del proyecto se incluyen varios archivos listos para automatizar el despliegue en servidores VPS:
- Archivos `.yml` preparados para subir la infraestructura completa a **CapRover** en 1 click (`caprover-todo-en-uno.yml`).
- Un archivo `docker-compose-local-unificado.yml` para levantar toda la arquitectura en una máquina local usando Docker Swarm/Compose.
