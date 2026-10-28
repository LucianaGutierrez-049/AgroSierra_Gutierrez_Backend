package pe.upeu.agrosierra_gutierrez.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import pe.upeu.agrosierra_gutierrez.web.dto.ErrorRespuesta

class Fallo(val estado: Int, override val message: String, val campos: Map<String,String> = emptyMap()) : RuntimeException(message)
@RestControllerAdvice
class ManejadorDeErrores {
    private fun respuesta(status: Int, mensaje: String, req: HttpServletRequest, campos: Map<String,String> = emptyMap()) =
        ResponseEntity.status(status).body(ErrorRespuesta(status=status, error=HttpStatus.valueOf(status).reasonPhrase,
            message=mensaje, path=req.requestURI, validationErrors=campos))
    @ExceptionHandler(Fallo::class)
    fun negocio(e: Fallo, req: HttpServletRequest) = respuesta(e.estado,e.message,req,e.campos)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException, req: HttpServletRequest) = respuesta(400,"Datos inválidos",req,
        e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") })
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun formato(e: Exception, req: HttpServletRequest) = respuesta(400,"Revisa el formato de los datos y la fecha (AAAA-MM-DD)",req)
    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorRespuesta> {
        org.slf4j.LoggerFactory.getLogger(javaClass).error("Error en ${req.requestURI}",e)
        return respuesta(500,"No pudimos completar la operación. Intenta nuevamente",req)
    }
}
