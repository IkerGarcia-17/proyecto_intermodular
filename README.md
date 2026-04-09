\# 🎾🎯 Proyecto PadelDart: Estado Actual y Hoja de Ruta



> \[cite\_start]\*\*Trabajo Fin de Grado\*\* desarrollado por Iker García Martínez\[cite: 41, 59]. 

> \[cite\_start]PadelDart es una aplicación que fusiona el mundo del pádel y los dardos\[cite: 1, 30]. \[cite\_start]Aunque la aplicación se encuentra en una fase temprana ("muy verde"), ya cuenta con una base sólida, elegante y práctica, con mucho margen para convertirse en una herramienta profesional\[cite: 21, 22, 222, 223].



\---



\## 📱 1. Estado Actual de la Aplicación (Puntos Finalizados)



\[cite\_start]Actualmente, el esqueleto principal de autenticación y recopilación de datos está operativo\[cite: 235]. Se han completado las siguientes funcionalidades:



\### 🔐 Inicio de Sesión y Autenticación

\* \[cite\_start]Se ha implementado una pantalla de \*\*Login\*\* con una estética basada en los colores de las pelotas de pádel\[cite: 10, 55].

\* \[cite\_start]Verifica si el usuario ya está registrado en la base de datos mediante un inicio de sesión estándar\[cite: 10, 55].

\* \[cite\_start]La pantalla de \*\*Recuperar Contraseña\*\* ya tiene la interfaz de diseño y la navegación integradas\[cite: 14, 160, 235]. \[cite\_start]Su objetivo será enviar un enlace de restablecimiento al correo del usuario\[cite: 14, 160].



\### 📝 Registro de Usuario (Completado en 4 Pasos)

\[cite\_start]Se ha dedicado gran parte del tiempo al flujo de registro para que sea lo más completo posible\[cite: 12, 82, 83]:

1\. \[cite\_start]\*\*Datos Personales:\*\* Recopila nombre, apellidos, DNI, correo electrónico y dirección\[cite: 12, 68, 69, 70, 71, 78]. \[cite\_start]La contraseña cuenta con validaciones de seguridad que se colorean al cumplirse los requisitos\[cite: 12, 73, 74, 75, 76, 85].

2\. \[cite\_start]\*\*Perfil de Pádel:\*\* Pide al usuario su nivel de juego (con guía de ayuda), posición en pista (Drive/Revés), provincia y club favorito mediante un sistema de listas\[cite: 12, 98, 100, 103, 104, 105, 109, 110, 111].

3\. \[cite\_start]\*\*Perfil de Dardos:\*\* Recoge información sobre si el usuario dispone de diana propia en casa y qué tipo de material utiliza (pelo o electrónica)\[cite: 12, 119, 120, 122, 131].

4\. \[cite\_start]\*\*Método de Pago:\*\* Permite seleccionar preferencias como Tarjeta, PayPal, Bizum o Efectivo, preparando la app para futuros servicios de mercado de segunda mano o reserva de pistas\[cite: 12, 141, 142, 145, 146, 147].



\### 💾 Base de Datos y APIs

\* \[cite\_start]\*\*Base de Datos Local:\*\* Se ha implementado una base de datos local provisional para realizar pruebas y almacenar los registros de los usuarios (datos personales, nivel de pádel, categorías, etc.)\[cite: 16, 17, 192, 193, 235].

\* \[cite\_start]\*\*API de Localización:\*\* Se ha integrado la API de \*OpenStreetMap\* (Nominatim) para buscar calles en España\[cite: 19, 204, 208, 220]. \[cite\_start]Esto permite que el Código Postal se autocomplete al buscar una dirección, facilitando el registro\[cite: 12, 86, 87].



\---



\## 🚀 2. Puntos Futuros (Hoja de Ruta a 3 Semanas)



\[cite\_start]Para las próximas 3 semanas, el desarrollo se centrará en expandir las funcionalidades principales y mejorar la experiencia de usuario\[cite: 235]:



\* \[cite\_start]\*\*Integración de terceros:\*\* Implementar la funcionalidad de Inicio de Sesión rápido utilizando Google y Apple (iOS)\[cite: 10, 56, 235].

\* \[cite\_start]\*\*Refinamiento del Registro:\*\* Añadir el campo de teléfono (actualmente faltante) y mejorar la estética general del paso correspondiente al Perfil de Dardos\[cite: 84, 236].

\* \[cite\_start]\*\*Navegación Principal:\*\* Diseñar y programar la pantalla principal de acceso inicial, incorporando un menú lateral para navegar por las distintas secciones de la aplicación\[cite: 237].

\* \[cite\_start]\*\*Módulo de Dardos:\*\* Iniciar y finalizar el apartado de dardos\[cite: 238]. \[cite\_start]Esto incluirá el sistema de puntuación y la posibilidad de crear partidas contra la Inteligencia Artificial o contra un amigo en modo local\[cite: 238].

\* \[cite\_start]\*\*Gestión de Perfil:\*\* Crear la pantalla de Ajustes y el Perfil de Usuario para que se puedan editar y modificar los datos previamente registrados\[cite: 239].

\* \[cite\_start]\*\*Comunicación:\*\* Comenzar la investigación y el desarrollo de un sistema de chat funcional, operando de manera provisional con la base de datos local\[cite: 240].



\---

\[cite\_start]\*Documentación estructurada con la ayuda de GeminiAI y diseños conceptualizados en Figma\*\[cite: 24, 25, 230, 231].

