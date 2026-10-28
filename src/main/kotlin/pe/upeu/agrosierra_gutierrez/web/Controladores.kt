package pe.upeu.agrosierra_gutierrez.web

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.http.ResponseEntity
import pe.upeu.agrosierra_gutierrez.config.Identidad
import pe.upeu.agrosierra_gutierrez.servicio.AgroServicio
import pe.upeu.agrosierra_gutierrez.web.dto.*
import java.net.URI

@RestController @RequestMapping("/api/v1")
class Controladores(private val servicio: AgroServicio) {
    @PostMapping("/auth/login") fun login(@Valid @RequestBody p: LoginPeticion) = servicio.login(p)
    @GetMapping("/usuarios/me") fun me(@AuthenticationPrincipal u: Identidad) = servicio.usuario(u.id).respuesta()
    @GetMapping("/lotes") fun lotes(@RequestParam(defaultValue="0") pagina: Int,
        @RequestParam(defaultValue="20") tamanio: Int, @RequestParam(required=false) valle: String?,
        @RequestParam(required=false) campaniaCerrada: Boolean?) = servicio.listar(pagina,tamanio,valle,campaniaCerrada)
    @GetMapping("/lotes/{id}") fun lote(@PathVariable id: Long) = servicio.lote(id)
    @GetMapping("/lotes/{id}/labores") fun labores(@PathVariable id: Long,
        @RequestParam(defaultValue="0") pagina: Int, @RequestParam(defaultValue="20") tamanio: Int) = servicio.labores(id,pagina,tamanio)
    @PostMapping("/labores") fun crear(@Valid @RequestBody p: LaborPeticion,@AuthenticationPrincipal u: Identidad): ResponseEntity<LaborRespuesta> {
        val labor=servicio.crear(p,u)
        return ResponseEntity.created(URI.create("/api/v1/labores/${labor.id}")).body(labor)
    }
    @PutMapping("/labores/{id}") fun editar(@PathVariable id: Long,@Valid @RequestBody p: LaborPeticion,
        @AuthenticationPrincipal u: Identidad) = servicio.editar(id,p,u)
    @DeleteMapping("/labores/{id}") fun eliminar(@PathVariable id: Long): ResponseEntity<Void> {
        servicio.eliminar(id); return ResponseEntity.noContent().build()
    }
    @PostMapping("/lotes/{id}/cerrar-campania") fun cerrar(@PathVariable id: Long) = servicio.cerrar(id)
}
