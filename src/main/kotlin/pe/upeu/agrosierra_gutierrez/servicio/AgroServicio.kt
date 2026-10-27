package pe.upeu.agrosierra_gutierrez.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pe.upeu.agrosierra_gutierrez.dominio.*
import pe.upeu.agrosierra_gutierrez.repositorio.*
import pe.upeu.agrosierra_gutierrez.config.*
import pe.upeu.agrosierra_gutierrez.web.dto.*
import java.time.LocalDateTime

@Service @Transactional
class AgroServicio(private val lotes: LoteRepository, private val labores: LaborRepository,
    private val usuarios: UsuarioRepository, private val claves: BCryptPasswordEncoder, private val jwt: JwtService) {
    fun login(p: LoginPeticion): SesionRespuesta {
        val u = usuarios.findByUsuario(p.usuario) ?: throw Fallo(401,"Usuario o contraseña incorrectos")
        if(!claves.matches(p.contrasena,u.clave)) throw Fallo(401,"Usuario o contraseña incorrectos")
        val (token,expira) = jwt.emitir(u)
        return SesionRespuesta(token,u.usuario,u.nombre,u.rol.nombre,u.rol.privilegios,u.id,expira,u.rol.edicionPropia)
    }
    fun usuario(id: Long) = usuarios.findById(id).orElseThrow { Fallo(401,"La sesión ya no es válida") }
    fun lote(id: Long): Lote = lotes.findById(id).orElseThrow { Fallo(404,"El lote no existe") }
    private fun pagina(p: Int,t: Int): PageRequest {
        if(p<0 || t !in 1..100) throw Fallo(400,"Página debe ser >= 0 y tamaño entre 1 y 100")
        return PageRequest.of(p,t,org.springframework.data.domain.Sort.by("id"))
    }
    fun listar(p: Int,t: Int,valle: String?,cerrada: Boolean?): Pagina<Lote> {
        val spec = Specification<Lote> { root,_,cb ->
            val filtros = mutableListOf<jakarta.persistence.criteria.Predicate>()
            if(!valle.isNullOrBlank()) filtros += cb.equal(root.get<String>("valle"),valle)
            if(cerrada!=null) filtros += cb.equal(root.get<Boolean>("campaniaCerrada"),cerrada)
            cb.and(*filtros.toTypedArray())
        }
        return lotes.findAll(spec,pagina(p,t)).respuesta { it }
    }
    fun labores(id: Long,p: Int,t: Int): Pagina<LaborRespuesta> {
        lote(id)
        val pageable = pagina(p,t).let { PageRequest.of(it.pageNumber,it.pageSize) }
        return labores.findByLoteIdOrderByFechaDescIdDesc(id,pageable).respuesta { it.respuesta() }
    }
    private fun validar(p: LaborPeticion,lote: Lote) {
        if(lote.campaniaCerrada) throw Fallo(409,"La campaña del lote está cerrada")
        if(!p.cantidad.isFinite() || p.cantidad<=0) throw Fallo(400,"Datos inválidos", mapOf("cantidad" to "Ingresa una cantidad mayor que cero"))
        val limite = lote.dosisMaximaPorHectarea*lote.hectareas
        if(p.tipo == TipoLabor.FUMIGACION && p.cantidad>limite)
            throw Fallo(400,"La fumigación supera la dosis permitida",mapOf("cantidad" to "La cantidad máxima para este lote es $limite"))
    }
    fun crear(p: LaborPeticion,identidad: Identidad): LaborRespuesta {
        // Idempotencia: un reintento tras perder la respuesta no inserta una segunda labor.
        if(!p.clienteId.isNullOrBlank()) labores.findByAutorIdAndClienteId(identidad.id,p.clienteId)?.let { return it.respuesta() }
        val lote = lote(p.loteId); validar(p,lote)
        return labores.save(Labor(lote=lote,tipo=p.tipo,fecha=p.fecha,autor=usuario(identidad.id),
            cantidad=p.cantidad,observacion=p.observacion,clienteId=p.clienteId?.takeIf { it.isNotBlank() })).respuesta()
    }
    fun editar(id: Long,p: LaborPeticion,identidad: Identidad): LaborRespuesta {
        val labor = labores.findById(id).orElseThrow { Fallo(404,"La labor no existe") }
        if(p.loteId != labor.lote.id) throw Fallo(400,"No se puede trasladar una labor a otro lote",mapOf("loteId" to "Conserva el lote original"))
        val usuario = usuario(identidad.id)
        if(usuario.rol.edicionPropia && (labor.autor.id != usuario.id || LocalDateTime.now(java.time.ZoneOffset.UTC).isAfter(labor.registradoEn.plusHours(24))))
            throw Fallo(403,"Solo puedes corregir tus propias labores durante las primeras 24 horas")
        validar(p,labor.lote)
        labor.tipo=p.tipo; labor.fecha=p.fecha; labor.cantidad=p.cantidad; labor.observacion=p.observacion
        return labor.respuesta()
    }
    fun eliminar(id: Long) {
        val labor = labores.findById(id).orElseThrow { Fallo(404,"La labor no existe") }
        labores.delete(labor)
    }
    fun cerrar(id: Long): Lote {
        val lote = lote(id)
        if(lote.campaniaCerrada) throw Fallo(409,"La campaña ya está cerrada")
        if(!labores.existsByLoteIdAndTipo(id,TipoLabor.COSECHA)) throw Fallo(409,"Registra al menos una cosecha antes de cerrar la campaña")
        lote.campaniaCerrada=true
        return lote
    }
}
