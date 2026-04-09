# 🎾🎯 Proyecto PadelDart: Gestión, Estadísticas y Aprendizaje

> **Trabajo Fin de Grado (DAM)** desarrollado por Iker García Martínez.

---

## 📖 ¿Qué es PadelDart? (Descripción del Proyecto)

**PadelDart** es una aplicación integral orientada principalmente a jugadores de pádel (con un módulo secundario dedicado a los dardos). Su objetivo principal es centralizar en una sola plataforma herramientas que habitualmente se encuentran dispersas en diferentes aplicaciones. 

La aplicación no solo busca ser una herramienta de gestión, sino una plataforma de **mejora deportiva y comunidad**. Entre sus características principales destacan:
* **Gestión de Partidos y Marcador en Vivo:** Permite crear partidos, invitar a otros jugadores y llevar la puntuación en tiempo real a través de un marcador digital intuitivo.
* **Estadísticas Avanzadas (Radar Charts):** Los usuarios pueden registrar sus golpes, victorias y derrotas. El sistema generará gráficos (tipo radar) para que el jugador visualice rápidamente sus fortalezas y debilidades en la pista.
* **Matchmaking y Comunidad:** Incorpora un buscador para encontrar jugadores de un nivel similar en la misma zona geográfica, fomentando la creación de partidas equilibradas.
* **Recursos de Aprendizaje:** En base a las estadísticas y los "puntos débiles" detectados, la app sugerirá recursos multimedia y vídeos para ayudar al usuario a mejorar su técnica.
* **Tecnologías:** Desarrollada de forma nativa en **Java** para dispositivos Android, con vistas a utilizar una arquitectura basada en **Firebase** (Firestore, Auth) para la gestión en la nube.

---

## 📱 1. Estado Actual de la Aplicación (Puntos Finalizados)

Actualmente, el esqueleto principal de la aplicación, centrado en la recopilación de datos, la experiencia de usuario inicial y la base de datos local, se encuentra operativo:

### 🔐 Inicio de Sesión y Autenticación
* **Pantalla de Login:** Se ha diseñado con una estética inmersiva utilizando los colores característicos de las pelotas de pádel. Cuenta con un inicio de sesión estándar que verifica las credenciales del usuario contra la base de datos.
* **Recuperar Contraseña:** Se ha implementado la interfaz y la navegación de esta pantalla. La funcionalidad (en proceso) permitirá al usuario introducir su correo electrónico para recibir un enlace seguro de restablecimiento de contraseña.

### 📝 Registro de Usuario Detallado (4 Fases)
Se ha diseñado un flujo de registro muy completo y escalonado para no saturar al usuario, vital para recopilar los datos necesarios para el *matchmaking*:
1. **Datos Personales y Seguridad:** Recopila información básica (nombre, DNI, correo, dirección). Destaca la validación dinámica de la contraseña: se muestran los requisitos de seguridad (mayúsculas, números, caracteres especiales) que cambian de color conforme el usuario los va cumpliendo.
2. **Perfil de Pádel:** Es el núcleo de la app. Solicita la posición preferida en pista (Drive o Revés) y el nivel del jugador (incluyendo un botón de información para guiarle). Además, utiliza un sistema de clave-valor para filtrar clubes favoritos en función de la provincia seleccionada.
3. **Perfil de Dardos:** Un módulo adicional donde el usuario indica si dispone de diana propia en casa y qué tipo de tecnología utiliza (pelo tradicional o electrónica).
4. **Preferencias de Pago:** Permite seleccionar métodos como Tarjeta, PayPal, Bizum o Efectivo. Este paso prepara la infraestructura para futuros módulos de la app, como la reserva de pistas, el pago a profesores o un mercado de segunda mano.

### 💾 Base de Datos y Consumo de APIs
* **Base de Datos Local Provisional:** Para agilizar las pruebas y el desarrollo actual, se ha implementado una base de datos local que almacena todos los datos del registro y categoriza a los jugadores según su nivel.
* **API de Localización (OpenStreetMap):** Se ha integrado la API de *Nominatim* mediante un hilo secundario en Java. Esto permite que, al escribir el nombre de una calle, el sistema busque direcciones en España y autocomplete el Código Postal automáticamente, mejorando enormemente la experiencia de usuario.

---

## 🚀 2. Hoja de Ruta (A corto plazo - 3 Semanas)

Para las próximas semanas de desarrollo, el trabajo se centrará en la navegación y la jugabilidad:

1. **Integración de Autenticación de Terceros:** Implementar el inicio de sesión rápido mediante Google y Apple (iOS).
2. **Refinamiento de UI/UX:** Añadir campos faltantes (como el teléfono) en el registro y mejorar el diseño visual del paso de dardos.
3. **Navegación Principal:** Diseñar la pantalla principal (Home) e incorporar un menú lateral (Navigation Drawer) que permita viajar fluidamente entre el Perfil, los Partidos, el Marcador y las Estadísticas.
4. **Módulo Activo de Dardos:** Desarrollar la lógica de puntuación para los dardos y permitir la creación de partidas locales contra otro amigo o contra una Inteligencia Artificial.
5. **Gestión de Perfil de Usuario:** Creación de la pantalla de Ajustes para poder visualizar y editar la información introducida durante el registro.
6. **Investigación de Chat:** Comenzar el diseño de un sistema de mensajería interna utilizando la base de datos provisional.

---

## 🔮 3. Mejoras Futuras (A largo plazo)

Una vez completada la estructura principal para el TFG, la aplicación tiene el siguiente margen de expansión:

* **Sistema de Reseñas:** Los usuarios podrán valorar a sus compañeros o rivales tras los partidos, evaluando tanto su nivel real como su deportividad.
* **Integración con Wearables:** Sincronización con pulseras de actividad (como la Padel Band o Apple Watch) para volcar estadísticas directamente a la app.
* **Análisis Gráfico y Vídeos:** Implementación de la librería de gráficos para los *Radar Charts* y conexión con una base de datos de vídeos explicativos para corregir errores frecuentes.
* **Bolsa de Empleo/Clases:** Una sección de anuncios donde academias y profesores particulares puedan ofertar vacantes o clases para los usuarios de la plataforma.