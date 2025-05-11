package pkgTarea4;

import java.util.Random;

public class Paginas {

    private static final String estilo = "<style>"
            + "body {font-family: Arial, sans-serif; background: #f0f2f5; margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; min-height: 100vh;}"
            + ".contenedor {background: white; padding: 2rem; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); width: 90%; max-width: 400px; text-align: center;}"
            + "h1, h2 {color: #333;}"
            + "form {display: flex; flex-direction: column;}"
            + "input, button {margin: 0.5rem 0; padding: 0.7rem; border-radius: 5px; border: 1px solid #ccc;}"
            + "button {background-color: #007bff; color: white; border: none; cursor: pointer;}"
            + "button:hover {background-color: #0056b3;}"
            + "a {color: #007bff; text-decoration: none;}"
            + "ul {list-style: none; padding: 0;}"
            + "li {margin: 0.5rem 0;}"
            + ".cerrar-sesion {position: absolute; top: 1rem; right: 1rem;}"
            + "</style>";

    public static final String html_index = "<html><head><title>Inicio</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
           // + "<a class='cerrar-sesion' href='/logout'>Cerrar sesión</a>"
            + "<div class='contenedor'>"
            + "<h1>Bienvenido al Servidor de Juegos</h1>"
            + "<ul>"
            + "<li><a href='/adivina'>Adivina el Número</a></li>"
            + "<li><a href='/dados'>Lanza Dados</a></li>"
            + "<li><a href='/ppt'>Piedra, Papel o Tijera</a></li>"
            + "</ul>"
            + "<p><a href='/logout'>Cerrar Sesión</a></p>"
            + "</div>"
            + "</body></html>";

    public static final String html_noEncontrado = "<html><head><title>Error 404</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
            //+ "<a class='cerrar-sesion' href='/logout'>Cerrar sesión</a>"
            + "<div class='contenedor'>"
            + "<h1>404 Página No Encontrada</h1>"
            + "<p>La página solicitada no existe.</p>"
            + "<p><a href='/logout'>Cerrar Sesión</a></p>"
            + "</div>"
            + "</body></html>";

    public static String generarHtmlAdivina(String resultado) {
        return "<html><head><title>Adivina el Número</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
                + "<div class='contenedor'>"
                + "<h1>¡Adivina el Número!</h1>"
                + "<form action='/adivina' method='POST'>"
                + "<label for='numero'>Introduce un número del 1 al 100:</label>"
                //+ "<input type='number' id='numero' name='numero' min='1' max='100' required pattern='\\d*'>"
                /*// PRUEBAS*/
                + "<input id='numero' name='numero' min='1' max='100'>"
                + "<button type='submit'>Enviar</button>"
                + "</form>"
                + resultado
                + "<p><a href='/logout'>Cerrar Sesión</a></p>"
                + "</div>"
                + "</body></html>";
    }

    public static String generarHtmlPpt(String resultado) {
        return "<html><head><title>Piedra, Papel o Tijera</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
               // + "<a class='cerrar-sesion' href='/logout'>Cerrar sesión</a>"
                + "<div class='contenedor'>"
                + "<h1>¡Juega a Piedra, Papel o Tijera!</h1>"
                + "<form action='/ppt' method='POST'>"
                + "<button name='opcion' value='Piedra' onclick='this.form.submit()'>Piedra</button>"
                + "<button name='opcion' value='Papel' onclick='this.form.submit()'>Papel</button>"
                + "<button name='opcion' value='Tijeras' onclick='this.form.submit()'>Tijeras</button>"
                + "</form>"
                + resultado
                + "<p><a href='/logout'>Cerrar Sesión</a></p>"
                + "</div>"
                + "</body></html>";
    }

    public static String generarHtmlDados(String resultado) {
        Random random = new Random();
        int dadoCliente = random.nextInt(6) + 1;
        return "<html><head><title>Lanza Dados</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
               // + "<a class='cerrar-sesion' href='/logout'>Cerrar sesión</a>"
                + "<div class='contenedor'>"
                + "<h1>¡Lanza Dados!</h1>"
                + "<form action='/dados' method='POST'>"
                + "<button name='lanzar' value='" + dadoCliente + "'>Lanzar Dados</button>"
                + "</form>"
                + resultado
                + "<p><a href='/logout'>Cerrar Sesión</a></p>"
                + "</div>"
                + "</body></html>";
    }

    public static final String html_login = "<html><head><title>Iniciar Sesión</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
            + "<div class='contenedor'>"
            + "<h2>Iniciar Sesión</h2>"
            + "<form action='/login' method='POST'>"
            + "<input type='email' name='email' placeholder='Correo electrónico' required>"
            + "<input type='password' name='password' placeholder='Contraseña' required>"
            + "<button type='submit'>Entrar</button>"
            + "</form>"
            + "<p>¿No tienes cuenta? <a href='/registro'>Regístrate aquí</a></p>"
            + "</div>"
            + "</body></html>";

    public static final String html_registro = "<html><head><title>Registro</title><meta charset=UTF-8><link rel=icon href=data:,/>" + estilo + "</head><body>"
            + "<div class='contenedor'>"
            + "<h2>Registro de Usuario</h2>"
            + "<form action='/registro' method='POST'>"
            + "<input type='text' name='nombre' placeholder='Nombre completo' required>"
            + "<input type='email' name='email' placeholder='Correo electrónico' required>"
            + "<input type='password' name='password' placeholder='Contraseña' required>"
            + "<input type='password' name='confirmar' placeholder='Confirmar contraseña' required>"
            + "<button type='submit'>Registrarse</button>"
            + "</form>"
            + "<p>¿Ya tienes cuenta? <a href='/login'>Inicia sesión</a></p>"
            + "</div>"
            + "</body></html>";

}
