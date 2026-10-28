package pe.upeu.agrosierra_gutierrez.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import pe.upeu.agrosierra_gutierrez.web.dto.ErrorRespuesta
import pe.upeu.agrosierra_gutierrez.dominio.Usuario
import java.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.time.Instant

data class Identidad(val id: Long, val privilegios: Set<String>)

@Component
class JwtService(private val mapper: ObjectMapper, @Value("\${agro.jwt.secret}") private val secret: String) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private fun firmar(texto: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")); doFinal(texto.toByteArray())
    }
    fun emitir(usuario: Usuario): Pair<String,Long> {
        val ahora = Instant.now().epochSecond
        val cabecera = encoder.encodeToString(mapper.writeValueAsBytes(mapOf("alg" to "HS256", "typ" to "JWT")))
        val cuerpo = encoder.encodeToString(mapper.writeValueAsBytes(mapOf("iss" to "AgroAPI", "sub" to usuario.id.toString(),
            "iat" to ahora, "exp" to ahora+3600, "privilegios" to usuario.rol.privilegios)))
        val datos = "$cabecera.$cuerpo"
        return "$datos.${encoder.encodeToString(firmar(datos))}" to (ahora+3600)*1000
    }
    fun verificar(token: String): Identidad {
        val partes = token.split('.')
        require(partes.size == 3)
        val decoder = Base64.getUrlDecoder()
        require(MessageDigest.isEqual(firmar("${partes[0]}.${partes[1]}"), decoder.decode(partes[2])))
        val cabecera = mapper.readTree(decoder.decode(partes[0]))
        require(cabecera.path("alg").asString() == "HS256")
        val cuerpo = mapper.readTree(decoder.decode(partes[1]))
        require(cuerpo.path("iss").asString() == "AgroAPI")
        require(cuerpo.path("exp").asLong() > Instant.now().epochSecond)
        return Identidad(cuerpo.path("sub").asString().toLong(), (mapper.readValue(decoder.decode(partes[1]), Map::class.java)["privilegios"] as List<*>).map { it.toString() }.toSet())
    }
}

@Component
class RespuestaSeguridad(private val mapper: ObjectMapper) {
    fun escribir(request: HttpServletRequest, response: HttpServletResponse, status: Int) {
        response.status = status; response.contentType = "application/json;charset=UTF-8"
        response.writer.write(mapper.writeValueAsString(ErrorRespuesta(status=status,
            error=if(status==401) "Unauthorized" else "Forbidden",
            message=if(status==401) "La sesión no es válida o ha vencido" else "No tienes el privilegio necesario",
            path=request.requestURI)))
    }
}

// Creado exclusivamente en la cadena de Spring Security: no se registra dos veces como filtro servlet.
class JwtAuthenticationFilter(private val jwt: JwtService, private val errores: RespuestaSeguridad) : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        if(req.servletPath == "/api/v1/auth/login") { chain.doFilter(req,res); return }
        val header = req.getHeader("Authorization")
        if(header != null) {
            val identidad = try { require(header.startsWith("Bearer ")); jwt.verificar(header.removePrefix("Bearer ")) }
                catch (_: Exception) { errores.escribir(req,res,401); return }
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                identidad, null, identidad.privilegios.map(::SimpleGrantedAuthority))
        }
        chain.doFilter(req,res)
    }
}

@Configuration
class SecurityConfig {
    @Bean fun passwordEncoder() = BCryptPasswordEncoder()
    @Bean fun security(http: HttpSecurity, jwt: JwtService, errores: RespuestaSeguridad): SecurityFilterChain {
        http.csrf { it.disable() }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint { req,res,_ -> errores.escribir(req,res,401) }
                .accessDeniedHandler { req,res,_ -> errores.escribir(req,res,403) } }
            .authorizeHttpRequests { it
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/lotes/*/labores").hasAuthority("LABOR_LEER")
                .requestMatchers(HttpMethod.GET, "/api/v1/lotes", "/api/v1/lotes/*").hasAuthority("LOTE_LEER")
                .requestMatchers(HttpMethod.POST, "/api/v1/labores").hasAuthority("LABOR_CREAR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/labores/*").hasAuthority("LABOR_EDITAR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/labores/*").hasAuthority("LABOR_ELIMINAR")
                .requestMatchers(HttpMethod.POST, "/api/v1/lotes/*/cerrar-campania").hasAuthority("CAMPANIA_CERRAR")
                .anyRequest().authenticated() }
            .addFilterBefore(JwtAuthenticationFilter(jwt,errores), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
