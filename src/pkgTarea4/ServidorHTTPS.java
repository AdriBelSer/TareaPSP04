package pkgTarea4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.*;

/**
 * Servidor HTTPS que maneja juegos interactivos.
 *
 * Rutas disponibles: - /adivina: Juega a "Adivina el N�mero". - /dados: Juega a
 * "Lanza Dados". - /ppt: Juega a "Piedra, Papel o Tijera".
 */
public class ServidorHTTPS {

    private static final ConcurrentHashMap<String, SesionJuego> sesiones = new ConcurrentHashMap<>();
    private static final int puerto = 8066;

    private static final Logger logger = Logger.getLogger("miLog");
    private static FileHandler fh;

    public static void main(String[] args) throws Exception {

        //CONFIGURACION DEL LOGGER
        try {
            fh = new FileHandler("logErrores.txt", true);
            fh.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd MM:mm:ss"));
                    return String.format("%s - %s%n", fechaHora, record.getMessage());
                }
            });
            logger.addHandler(fh);
            logger.setUseParentHandlers(true);
            //Hook para cerrar el FileHandler al finalizar la aplicación
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (fh != null) {
                    fh.close();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();

        }
        SimpleFormatter formatter = new SimpleFormatter();

        fh.setFormatter(formatter);

        //SEGURIDAD SSL
        //Cargar el almacen de claves (keystore)
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyFile = new FileInputStream("AlmacenSSL")) {
            keyStore.load(keyFile, "123456".toCharArray());
        }

        //Inicializar el gestor de claves con el keystore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "123456".toCharArray());

        //Inicializar el contexto SSL con el gestor de claves 
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        //Declara objeto tipo Factory para crear socket SSL servidor
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

        //Crea un socket servidor seguro
        SSLServerSocket socketServidorSsl = (SSLServerSocket) factory.createServerSocket(puerto);
        System.out.println("Servidor SSL escuchando en el puerto: " + puerto);
        System.out.println("Visita https://localhost:8066");

        while (true) {
            SSLSocket socketSsl = (SSLSocket) socketServidorSsl.accept();
            System.out.println("Cliente conectado");

            Thread hiloCliente = new HiloCliente(socketSsl); //Crea un nuevo hilo para manejar al cliente
            hiloCliente.start();
        }

    }

    /**
     * Clase interna que implementa la l�gica de manejar un cliente. Extiende la
     * clase Thread y sobrescribe el m�todo run.
     */
    private static class HiloCliente extends Thread {

        private final Socket cliente;
        private static final String ALGORITMO = "AES";
        private static final byte[] CLAVE_AES = "1234567890123456".getBytes();

        public HiloCliente(Socket cliente) {
            this.cliente = cliente; // Asocia el socket del cliente al hilo.
        }

        @Override
        public void run() {
            try (
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream())); PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true, StandardCharsets.UTF_8)) {
                // Lee la primera l�nea de la petición HTTP.
                String peticion = entrada.readLine();
                if (peticion == null || (!peticion.startsWith("GET") && !peticion.startsWith("POST"))) {
                    return; // Ignora la petici�n si no es GET o POST.
                }
                System.out.println("peticion: " + peticion);
                String ruta = peticion.split(" ")[1]; // Extrae la ruta solicitada.

                // Leer encabezados HTTP. Determina la sesionID y el tama�o del cuerpo.
                String[] metadatos = new String[2];
                metadatos = obtenerMetadatos(entrada);
                String sessionId = metadatos[0];
                SesionJuego sesion = sesiones.computeIfAbsent(sessionId, k -> new SesionJuego());

                int contentLength = Integer.parseInt(metadatos[1]);

                System.out.println("linea: vacia");

                // Leer el cuerpo si es un POST.
                StringBuilder cuerpo = new StringBuilder(); // Para almacenar el cuerpo de la solicitud.
                if (peticion.startsWith("POST") && contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    entrada.read(buffer, 0, contentLength);
                    cuerpo.append(buffer);
                }

                String respuesta; // Contendr� la respuesta generada por el servidor.
                if (ruta.equals("/health")) {
                    respuesta = manejarHealth(sessionId);
                }
                else if (ruta.equals("/")) {
                    if (sesion.estaAutenticado()) {
                        respuesta = construirRespuesta(200, Paginas.html_index, sessionId);
                    } else {
                        respuesta = manejarLogin(cuerpo.toString(), sesion, sessionId);
                    }
                } else if (ruta.equals("/login")) {
                    respuesta = manejarLogin(cuerpo.toString(), sesion, sessionId);
                } else if (ruta.equals("/registro")) {
                    if (sesion.estaAutenticado()) {
                        respuesta = construirRespuesta(200, Paginas.html_index, sessionId);
                    } else {
                        respuesta = manejarRegistro(cuerpo.toString(), sesion, sessionId);
                    }
                } else if (ruta.startsWith("/adivina")) {
                    if (!sesion.estaAutenticado()) {
                        respuesta = manejarLogin(cuerpo.toString(), sesion, sessionId);
                    } else {
                        respuesta = manejarAdivina(cuerpo.toString(), sesion, sessionId);
                    }
                } else if (ruta.startsWith("/dados")) {
                    if (!sesion.estaAutenticado()) {
                        respuesta = manejarLogin(cuerpo.toString(), sesion, sessionId);
                    } else {
                        respuesta = manejarDados(cuerpo.toString(), sesion, sessionId);
                    }
                } else if (ruta.startsWith("/ppt")) {
                    if (!sesion.estaAutenticado()) {
                        respuesta = manejarLogin(cuerpo.toString(), sesion, sessionId);
                    } else {
                        respuesta = manejarPPT(cuerpo.toString(), sesion, sessionId);
                    }
                } else if (ruta.startsWith("/logout")) {
                    sesiones.remove(sessionId);
                    respuesta = construirRedirect("/", sessionId);
                } else {
                    respuesta = construirRespuesta(404, Paginas.html_noEncontrado, sessionId);
                }

                salida.println(respuesta); // Env�a la respuesta al cliente.
            } catch (IOException e) {
                e.printStackTrace(); // Muestra errores en la consola.
            }
        }

        private String[] obtenerMetadatos(BufferedReader entrada) throws IOException {
            String linea;
            String[] metadatos = new String[2];
            String sessionId = null;
            String contentLength = "0";

            while (!(linea = entrada.readLine()).isBlank()) {
                System.out.println("Metadato: " + linea);
                if (linea.startsWith("Cookie: ")) {
                    String[] cookies = linea.substring(8).split("; ");
                    for (String cookie : cookies) {
                        if (cookie.startsWith("sessionId=")) {
                            sessionId = cookie.substring(10);
                            break;
                        }
                    }
                } else if (linea.startsWith("Content-Length: ")) {
                    contentLength = linea.substring(16);
                }
            }

            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }
            System.out.println("COOKIE: " + sessionId);

            metadatos[0] = sessionId;
            metadatos[1] = contentLength;
            return metadatos;
        }

        private String manejarAdivina(String cuerpo, SesionJuego sesion, String sessionId) {
            if (sesion.numeroSecreto == 0) {
                sesion.numeroSecreto = new Random().nextInt(100) + 1; // Genera un n�mero aleatorio al iniciar el juego.
                sesion.intentosAdivina = 0; // Resetea los intentos.
            }

            int codigo = 200;
            String respuestaHTML;
            try {
                if (!cuerpo.isEmpty()) {
                    System.out.println("Cuerpo Adivina: " + cuerpo);
                    int numeroUsuario = Integer.parseInt(cuerpo.split("=")[1]);
                    sesion.intentosAdivina++;

                    if (numeroUsuario == sesion.numeroSecreto) {
                        respuestaHTML = "<p>¡Felicidades! Has acertado el número " + sesion.numeroSecreto + " en " + sesion.intentosAdivina + " intentos.</p>";
                        sesion.numeroSecreto = 0; // Reinicia el juego.
                    } else if (sesion.intentosAdivina == 10) {
                        respuestaHTML = "<p>No has acertado en 10 intentos. El número era " + sesion.numeroSecreto + ". Pulsa <a href='/adivina'>aquí</a> para reiniciar el juego.</p>";
                        sesion.numeroSecreto = 0; // Reinicia el juego.
                    } else {
                        respuestaHTML = "<p>Intento " + sesion.intentosAdivina + ": El número es " + (numeroUsuario < sesion.numeroSecreto ? "mayor" : "menor") + ".</p>";
                    }
                } else {
                    respuestaHTML = "<p>Introduce un número para empezar el juego.</p>";
                }
            } catch (Exception e) {
                String valorRecibido = cuerpo.split("=").length > 1 ? cuerpo.split("=")[1] : "N/A";
                int lineaError = e.getStackTrace()[0].getLineNumber();
                logger.log(Level.SEVERE, String.format("Error juego Adivina en la linea %d: El valor introducido no es correcto. Valor recibido: %s", lineaError, valorRecibido));
                respuestaHTML = "<p>Error procesando tu número. Intenta de nuevo.  </p>";
                codigo = 400;
                e.printStackTrace(); // Muestra errores en la consola.
            }

            return construirRespuesta(200, Paginas.generarHtmlAdivina(respuestaHTML), sessionId);
        }

        private String manejarDados(String cuerpo, SesionJuego sesion, String sessionId) {
            String resultado;
            int codigo = 200;
            System.out.println("cuerpoDados:" + cuerpo);

            try {
                if (!cuerpo.isEmpty()) {
                    Random random = new Random();
                    int dadoUsuario = Integer.parseInt(cuerpo.split("=")[1]);
                    int dadoServidor = random.nextInt(6) + 1;

                    if (dadoUsuario > dadoServidor) {
                        sesion.marcadorUsuarioDados++;
                        sesion.rondaDados++;
                        resultado = "<p>Ronda " + sesion.rondaDados + " .Ganaste esta ronda. Tu dado: " + dadoUsuario + " - Dado del servidor: " + dadoServidor + "  </p>    ";
                    } else if (dadoUsuario < dadoServidor) {
                        sesion.marcadorServidorDados++;
                        sesion.rondaDados++;
                        resultado = "<p>Ronda " + sesion.rondaDados + " .Perdiste esta ronda. Tu dado: " + dadoUsuario + " - Dado del servidor: " + dadoServidor + "  </p>    ";
                    } else {
                        resultado = "<p>Ronda " + sesion.rondaDados + " .Empate en esta ronda. Ambos sacaron: " + dadoUsuario + "  </p>   ";
                    }

                    if (sesion.rondaDados == 5) {
                        if (sesion.marcadorUsuarioDados > sesion.marcadorServidorDados) {
                            resultado += "<p>¡Ganaste el juego! Marcador final: " + sesion.marcadorUsuarioDados + " - " + sesion.marcadorServidorDados + ". Vuelve a pulsar el bot�n para jugar de nuevo.</p>     ";
                        } else {
                            resultado += "<p>Perdiste el juego. Marcador final: " + sesion.marcadorUsuarioDados + " - " + sesion.marcadorServidorDados + ". Vuelve a pulsar el bot�n para jugar de nuevo.</p>     ";
                        }
                        sesion.marcadorUsuarioDados = 0;
                        sesion.marcadorServidorDados = 0;
                        sesion.rondaDados = 0;
                    }
                } else {
                    resultado = "<p>Pulsa el botón para lanzar los dados.  </p>    ";
                }

            } catch (Exception e) {
                resultado = "<p>Error procesando tu elección. Intenta de nuevo.</p>";
                codigo = 400;
                e.printStackTrace(); // Muestra errores en la consola.
            }

            return construirRespuesta(200, Paginas.generarHtmlDados(resultado), sessionId);
        }

        private String manejarPPT(String cuerpo, SesionJuego sesion, String sessionId) {

            String[] opciones = {"Piedra", "Papel", "Tijeras"};
            String resultado = "<p>Pulsa un botón para jugar.</p>  ";
            int codigo = 200;

            System.out.println("cuerpoPPT:" + cuerpo);
            try {
                if (!cuerpo.isEmpty()) {
                    String eleccionUsuario = cuerpo.split("=")[1];
                    if (!Arrays.asList("Piedra", "Papel", "Tijeras").contains(eleccionUsuario)) {
                        throw new IllegalArgumentException("Opción no válida");
                    }
                    String eleccionServidor = opciones[new Random().nextInt(3)];

                    if (eleccionUsuario.equals(eleccionServidor)) {
                        resultado = "<p>Ronda " + sesion.rondaPPT + " .Empate en esta ronda. Ambos eligieron: " + eleccionServidor + "</p>  ";
                    } else if ((eleccionUsuario.equals("Piedra") && eleccionServidor.equals("Tijeras"))
                            || (eleccionUsuario.equals("Papel") && eleccionServidor.equals("Piedra"))
                            || (eleccionUsuario.equals("Tijeras") && eleccionServidor.equals("Papel"))) {
                        sesion.marcadorUsuarioPPT++;
                        sesion.rondaPPT++;
                        resultado = "<p>Ronda " + sesion.rondaPPT + " .Ganaste esta ronda. Elegiste: " + eleccionUsuario + " - El servidor eligi�: " + eleccionServidor + "  </p>  ";
                    } else {
                        sesion.marcadorServidorPPT++;
                        sesion.rondaPPT++;
                        resultado = "<p>Ronda " + sesion.rondaPPT + " .Perdiste esta ronda. Elegiste: " + eleccionUsuario + " - El servidor eligi�: " + eleccionServidor + "  </p>  ";
                    }

                    if (sesion.rondaPPT == 5) {
                        if (sesion.marcadorUsuarioPPT > sesion.marcadorServidorPPT) {
                            resultado += "<p>¡Ganaste el juego! Marcador final: " + sesion.marcadorUsuarioPPT + " - " + sesion.marcadorServidorPPT + ". Vuelve a pulsar un bot�n para jugar de nuevo.</p>    ";
                        } else {
                            resultado += "<p>Perdiste el juego. Marcador final: " + sesion.marcadorUsuarioPPT + " - " + sesion.marcadorServidorPPT + ". Vuelve a pulsar un bot�n para jugar de nuevo.</p>    ";
                        }
                        sesion.marcadorUsuarioPPT = 0;
                        sesion.marcadorServidorPPT = 0;
                        sesion.rondaPPT = 0;
                    }
                }
            } catch (Exception e) {
                String valorRecibido = cuerpo.split("=").length > 1 ? cuerpo.split("=")[1] : "N/A";
                int lineaError = e.getStackTrace()[0].getLineNumber();
                logger.log(Level.SEVERE, String.format("Error juego Piedra, papel, o tijeras en la linea %d: El valor introducido no es correcto. Valor recibido: %s", lineaError, valorRecibido));
                resultado = "<p>Error procesando tu elección. Intenta de nuevo.</p>  ";
                codigo = 400;
                e.printStackTrace(); // Muestra errores en la consola.
            }

            return construirRespuesta(codigo, Paginas.generarHtmlPpt(resultado), sessionId);
        }

        private String construirRespuesta(int codigo, String contenido, String sessionId) {
            return (codigo == 200 ? "HTTP/1.1 200 OK" : "HTTP/1.1 400 Bad Request") + "\r\n" //Linea inicial
                    + "Content-Type: text/html; charset=UTF-8" + "\r\n" //Metadatos
                    + "Content-Length: " + contenido.length() + "\r\n"
                    + "Set-Cookie: sessionId=" + sessionId + "; Path=/;\r\n"
                    + "\r\n" //L�nea vac�a
                    + contenido;                                                             //Cuerpo
        }

        private String manejarLogin(String cuerpo, SesionJuego sesion, String sessionId) {
            try {
                if (cuerpo.isEmpty()) {
                    return construirRespuesta(200, Paginas.html_login, sessionId);
                }

                Map<String, String> params = parseParams(cuerpo);
                String email = params.get("email");
                String password = params.get("password");

                // Validar formato
                if (!validarEmail(email) || !validarPassword(password)) {
                    return construirRespuesta(400, Paginas.html_login + "<p style='color:red;'>Credenciales inválidas</p>", sessionId);
                }

                // Verificar usuario
                if (verificarUsuario(email, password)) {
                    sesion.autenticar(email);
                    return construirRedirect("/", sessionId);
                } else {
                    return construirRespuesta(401, Paginas.html_login + "<p style='color:red;'>Usuario o contraseña incorrectos</p>", sessionId);
                }

            } catch (Exception e) {
                return construirRespuesta(500, Paginas.html_login + "<p style='color:red;'>Error en el servidor</p>", sessionId);
            }
        }

        private String manejarRegistro(String cuerpo, SesionJuego sesion, String sessionId) {
            try {
                if (cuerpo.isEmpty()) {
                    return construirRespuesta(200, Paginas.html_registro, sessionId);
                }

                Map<String, String> params = parseParams(cuerpo);
                String email = params.get("email");
                String password = params.get("password");
                String confirmar = params.get("confirmar");

                // Validaciones
                if (!password.equals(confirmar)) {
                    return construirRespuesta(400, Paginas.html_registro + "<p style='color:red;'>Las contraseñas no coinciden</p>", sessionId);
                }
                if (!validarEmail(email) || !validarPassword(password)) {
                    return construirRespuesta(400, Paginas.html_registro + "<p style='color:red;'>Formato inválido</p>", sessionId);
                }
                if (usuarioYaRegistrado(email)) {
                    return construirRespuesta(409, Paginas.html_registro + "<p style='color:red;'>El usuario ya existe</p>", sessionId);
                }

                // Guardar usuario
                if (registrarUsuario(email, password)) {
                    sesion.autenticar(email);
                    return construirRedirect("/", sessionId);
                } else {
                    return construirRespuesta(409, Paginas.html_registro + "<p style='color:red;'>Usuario ya existe</p>", sessionId);
                }
            } catch (Exception e) {
                return construirRespuesta(500, Paginas.html_registro + "<p style='color:red;'>Error en el servidor</p>", sessionId);
            }
        }

        private boolean usuarioYaRegistrado(String email) throws IOException {
            File f = new File("usuarios.txt");
            if (!f.exists()) {
                return false;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    //Descifrar
                    String lineaDescifrada = descifrar(Base64.getDecoder().decode(linea));
                    if (lineaDescifrada.startsWith(email + ":")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private synchronized boolean verificarUsuario(String email, String password) throws IOException {
            Path path = Paths.get("usuarios.txt");
            if (!Files.exists(path)) {
                return false;
            }

            try (BufferedReader br = Files.newBufferedReader(path)) {
                String lineaCifrada;
                while ((lineaCifrada = br.readLine()) != null) {
                    //Descifrar linea
                    String lineaDescifrada = descifrar(Base64.getDecoder().decode(lineaCifrada));
                    String[] partes = lineaDescifrada.split(":", 2);
                    if (partes.length == 2 && partes[0].equals(email)) {
                        return BCrypt.checkpw(password, partes[1]);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;

        }

        private synchronized boolean registrarUsuario(String email, String password) throws IOException {

            //Generar hash de la contraseña
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            //Cifrar email
            String datos = email + ":" + hashedPassword;
            try {
                byte[] datosCifrados = cifrar(datos);
                String lineaCifrada = Base64.getEncoder().encodeToString(datosCifrados);
                try (PrintWriter out = new PrintWriter(new FileWriter("usuarios.txt", true))) {
                    out.println(lineaCifrada);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
// TODO 

        private String manejarLogout(SesionJuego sesion, String sessionId) {
            sesion.cerrarSesion();
            sesiones.remove(sessionId);
            return "HTTP/1.1 302 Found\n"
                    + "Location: /login\n"
                    + "Set-Cookie: sessionId=; Path=/; Max-Age=0\n\n";
        }

        private Map<String, String> parseParams(String cuerpo) {
            return Arrays.stream(cuerpo.split("&"))
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(
                            arr -> URLDecoder.decode(arr[0], StandardCharsets.UTF_8),
                            arr -> {
                                try {
                                    return arr.length > 1
                                            ? URLDecoder.decode(arr[1], StandardCharsets.UTF_8) : "";
                                } catch (Exception e) {
                                    return "";
                                }
                            }
                    ));
        }

        private boolean validarEmail(String email) {
            return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
        }

        private boolean validarPassword(String password) {
            return password != null && password.matches("^[a-zA-Z0-9]{6,}$");
        }

        /*TODO
        private String construirRedirect(String urlDestino, String sessionId) {
            try {
                String encodedUrl = URLEncoder.encode(urlDestino, StandardCharsets.UTF_8.toString());
                return "HTTP/1.1 302 Found\n"
                        + "Location: " + encodedUrl + "\n"
                        + "Set-Cookie: sessionId=" + sessionId + "; Path=/; HttpOnly\n"
                        + "\n";
            } catch (Exception e) {
                return construirRespuesta(500, "Error en redirección", sessionId);
            }
        }*/
        private String construirRedirect(String urlDestino, String sessionId) {
            return "HTTP/1.1 302 Found\r\n"
                    + "Location: " + urlDestino + "\r\n" // <-- Elimina URLEncoder.encode()
                    + "Set-Cookie: sessionId=" + sessionId + "; Path=/; HttpOnly\r\n"
                    + "\r\n";
        }

        private static byte[] cifrar(String datos) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKey clave = new SecretKeySpec(CLAVE_AES, ALGORITMO);
            //Se inicializa el cifrador en modo CIFRADO o ENCRIPTACIÓN 
            cipher.init(Cipher.ENCRYPT_MODE, clave);
            return cipher.doFinal(datos.getBytes());
        }

        private static String descifrar(byte[] datosCifrados) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKey clave = new SecretKeySpec(CLAVE_AES, ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave);
            return new String(cipher.doFinal(datosCifrados));
        }

        private String manejarHealth(String sessionId) {
            String html = "<html><head><title>Health Check</title></head>"
                    + "<body><h1>Servidor operativo</h1></body></html>";
            return construirRespuesta(200, html, sessionId);
        }

    }

}
