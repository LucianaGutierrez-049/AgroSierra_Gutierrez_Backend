package pe.upeu.agrosierra_gutierrez.web.dto

import jakarta.validation.constraints.*
import pe.upeu.agrosierra_gutierrez.dominio.*
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Page

data class LoginPeticion(@field:NotBlank val usuario: String = "", @field:NotBlank val contrasena: String = "")
data class UsuarioRespuesta(val id: Long, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>)
data class SesionRespuesta(val token: String, val usuario: String, val nombre: String, val rol: String,
    val privilegios: Set<String>, val usuarioId: Long, val expiraEn: Long, val edicionPropia: Boolean)
data class LaborPeticion(
    @field:Positive val loteId: Long = 0,
    val tipo: TipoLabor = TipoLabor.RIEGO,
    val fecha: LocalDate = LocalDate.now(),
    @field:Positive val cantidad: Double = 0.0,
    @field:Size(max=1000) val observacion: String = "",
    @field:Size(max=80) val clienteId: String? = null
)
data class LaborRespuesta(val id: Long, val loteId: Long, val tipo: TipoLabor, val fecha: LocalDate,
    val responsable: String, val cantidad: Double, val observacion: String, val pendiente: Boolean = false,
    val autorId: Long, val registradoEn: LocalDateTime)
fun Labor.respuesta() = LaborRespuesta(id, lote.id, tipo, fecha, autor.nombre, cantidad, observacion, false, autor.id, registradoEn)
fun Usuario.respuesta() = UsuarioRespuesta(id, usuario, nombre, rol.nombre, rol.privilegios)
data class Pagina<T>(val contenido: List<T>, val pagina: Int, val tamanio: Int,
    val totalElementos: Long, val totalPaginas: Int, val ultima: Boolean)
fun <T: Any,R> Page<T>.respuesta(convertir: (T)->R) = Pagina(content.map(convertir), number, size, totalElements, totalPages, isLast)
data class ErrorRespuesta(val timestamp: String = LocalDateTime.now().toString(), val status: Int,
    val error: String, val message: String, val path: String, val validationErrors: Map<String,String> = emptyMap())
