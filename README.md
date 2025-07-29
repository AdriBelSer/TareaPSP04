# Servidor HTTPS con Juegos

Este proyecto es un servidor HTTPS seguro que implementa un sistema de autenticación de usuarios y tres juegos interactivos: "Adivina el número", "Juego de dados" y "Piedra, papel o tijeras".

## Características principales
🔒 Conexiones seguras mediante SSL/TLS
👤 Sistema de registro y autenticación de usuarios
🎯 Tres juegos interactivos:

Adivina el número: Intenta adivinar un número entre 1-100 en 10 intentos
Dados: Compite contra el servidor en 5 rondas
Piedra, papel o tijeras: Juega al clásico juego contra la máquina

📝 Sistema de logging para seguimiento de errores
🔄 Manejo de sesiones con cookies
🛡️ Almacenamiento seguro de credenciales

## Requisitos previos
Java JDK 17 o superior
Biblioteca jBCrypt (incluida en las dependencias)
Archivo de almacén de claves SSL (AlmacenSSL)

## Configuración e instalación
Generar el almacén de claves SSL:
bash
keytool -genkeypair -alias mydomain -keyalg RSA -keystore AlmacenSSL -storepass 123456
Complete los datos solicitados para generar el certificado.

Compilar el proyecto:
bash
javac -cp .;bcrypt-0.4.jar pkgTarea4/ServidorHTTPS.java

Ejecutar el servidor:
bash
java -cp .;bcrypt-0.4.jar pkgTarea4.ServidorHTTPS

## Uso del sistema
1- Accede al servidor en tu navegador:
https://localhost:8066

2- Flujo de usuario:
Registra una nueva cuenta con email y contraseña
Inicia sesión con tus credenciales
Selecciona uno de los juegos disponibles
Juega y disfruta de la experiencia
Cierra sesión cuando termines

## Seguridad implementada
🔐 Cifrado AES para datos sensibles
🧂 Hashing bcrypt para contraseñas (coste 12)
🍪 Cookies HttpOnly para gestión de sesiones
🔑 Validación de formato para emails y contraseñas
🛡️ Semaforización para acceso concurrente seguro a archivos
📊 Logging detallado de errores y eventos

## Tecnologías utilizadas
Java SE 17
SSL/TLS para comunicaciones seguras
jBCrypt para hashing de contraseñas
Java Cryptography Architecture (JCA)
Java Secure Socket Extension (JSSE)
Java Logging API

## Capturas de pantalla
<img width="400" height="400" alt="Screenshot 1" src="https://github.com/user-attachments/assets/a4c0b709-f3f8-442f-8946-7f676d8684fd" />
<img width="400" height="400" alt="Screenshot 2" src="https://github.com/user-attachments/assets/c542c9ec-5bab-4f6d-9167-109525251e13" />
<img width="400" height="400" alt="Screenshot 3" src="https://github.com/user-attachments/assets/d6368931-c8f2-4b50-97df-7a2cedea9093" />
<img width="400" height="400" alt="Screenshot 4" src="https://github.com/user-attachments/assets/a4ef442a-7e86-46f2-8efc-ef2a37780ba3" />
<img width="400" height="400" alt="Screenshot 5" src="https://github.com/user-attachments/assets/7d39921e-410e-4b37-ad2f-8c463166b4a7" />

