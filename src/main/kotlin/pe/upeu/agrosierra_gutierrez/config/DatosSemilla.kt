package pe.upeu.agrosierra_gutierrez.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pe.upeu.agrosierra_gutierrez.dominio.*
import pe.upeu.agrosierra_gutierrez.repositorio.*
import java.time.LocalDate

@Component
class DatosSemilla(private val roles: RolRepository,private val usuarios: UsuarioRepository,
    private val lotes: LoteRepository,private val labores: LaborRepository,private val encoder: BCryptPasswordEncoder) : ApplicationRunner {
    @Transactional override fun run(args: ApplicationArguments) {
        if(usuarios.count()>0) return
        val lectura = setOf("LOTE_LEER","LABOR_LEER")
        val tecnico = roles.save(Rol("TECNICO",true,(lectura+setOf("LABOR_CREAR","LABOR_EDITAR")).toMutableSet()))
        val jefe = roles.save(Rol("JEFE_CAMPO",false,(lectura+setOf("LABOR_CREAR","LABOR_EDITAR","LABOR_ELIMINAR","CAMPANIA_CERRAR")).toMutableSet()))
        val admin = roles.save(Rol("ADMINISTRADOR",false,(lectura+"USUARIO_GESTIONAR").toMutableSet()))
        val clave = requireNotNull(encoder.encode("Agro2026*"))
        val u = usuarios.save(Usuario(usuario="tecnico1",nombre="Lucía · Técnica de campo",clave=clave,rol=tecnico))
        val j = usuarios.save(Usuario(usuario="jefe1",nombre="Jefatura de campo",clave=clave,rol=jefe))
        usuarios.save(Usuario(usuario="admin",nombre="Administración AgroSierra",clave=clave,rol=admin))
        val nombres=listOf("Las Palmeras","El Mirador","Los Eucaliptos","La Esperanza","Valle Verde","Los Pinos","El Retiro","Santa Rosa")
        val valles=listOf("Mantaro","Urubamba","Colca")
        val cultivos=listOf("Papa","Maíz","Quinua","Haba")
        for(i in 0..7) {
            val lote=lotes.save(Lote(codigo="LOT-${(i+1).toString().padStart(3,'0')}",nombre=nombres[i],
                hectareas=2.0+i*0.5,cultivo=cultivos[i%4],valle=valles[i%3],campaniaCerrada=i>=6,dosisMaximaPorHectarea=2.5))
            val cantidad=if(i<4) 3 else 2
            repeat(cantidad) { n ->
                val tipo=if(i==0 && n==2) TipoLabor.COSECHA else if(n==1) TipoLabor.FUMIGACION else TipoLabor.RIEGO
                labores.save(Labor(lote=lote,tipo=tipo,fecha=LocalDate.now().minusDays(n.toLong()),
                    autor=if(n%2==0) u else j,cantidad=if(tipo==TipoLabor.FUMIGACION) 2.0 else 10.0,
                    observacion="Labor de campaña registrada en ${nombres[i]}"))
            }
        }
    }
}
