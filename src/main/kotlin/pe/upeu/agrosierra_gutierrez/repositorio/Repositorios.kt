package pe.upeu.agrosierra_gutierrez.repositorio

import pe.upeu.agrosierra_gutierrez.dominio.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RolRepository : JpaRepository<Rol, String>
interface UsuarioRepository : JpaRepository<Usuario, Long> { fun findByUsuario(usuario: String): Usuario? }
interface LoteRepository : JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote>
interface LaborRepository : JpaRepository<Labor, Long> {
    fun findByLoteIdOrderByFechaDescIdDesc(loteId: Long, pageable: Pageable): Page<Labor>
    fun existsByLoteIdAndTipo(loteId: Long, tipo: TipoLabor): Boolean
    fun findByAutorIdAndClienteId(autorId: Long, clienteId: String): Labor?
}
