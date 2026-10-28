package pe.upeu.agrosierra_gutierrez.dominio

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity @Table(name="rol")
class Rol(
    @Id var nombre: String = "",
    var edicionPropia: Boolean = false,
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="rol_privilegio", joinColumns=[JoinColumn(name="rol_nombre")])
    @Column(name="privilegio") var privilegios: MutableSet<String> = mutableSetOf()
)

@Entity @Table(name="usuario")
class Usuario(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id: Long = 0,
    @Column(unique=true) var usuario: String = "",
    var nombre: String = "",
    var clave: String = "",
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="rol_nombre") var rol: Rol = Rol()
)

@Entity @Table(name="lote")
class Lote(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id: Long = 0,
    @Column(unique=true) var codigo: String = "",
    var nombre: String = "",
    var hectareas: Double = 0.0,
    var cultivo: String = "",
    var valle: String = "",
    var campaniaCerrada: Boolean = false,
    var dosisMaximaPorHectarea: Double = 0.0
)

enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }

@Entity @Table(name="labor", uniqueConstraints=[UniqueConstraint(columnNames=["autor_id", "cliente_id"])])
class Labor(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id: Long = 0,
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="lote_id") var lote: Lote = Lote(),
    @Enumerated(EnumType.STRING) var tipo: TipoLabor = TipoLabor.RIEGO,
    var fecha: LocalDate = LocalDate.now(),
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="autor_id") var autor: Usuario = Usuario(),
    var cantidad: Double = 0.0,
    @Column(length=1000) var observacion: String = "",
    var registradoEn: LocalDateTime = LocalDateTime.now(java.time.ZoneOffset.UTC),
    @Column(name="cliente_id") var clienteId: String? = null
)
