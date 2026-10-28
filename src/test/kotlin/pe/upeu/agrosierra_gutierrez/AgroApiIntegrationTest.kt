package pe.upeu.agrosierra_gutierrez

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import tools.jackson.databind.ObjectMapper

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgroApiIntegrationTest {
    @LocalServerPort var port: Int = 0
    @Autowired lateinit var mapper: ObjectMapper
    private val client=HttpClient.newHttpClient()
    private fun request(method: String,path: String,token: String?=null,body: String?=null): HttpResponse<String> {
        val builder=HttpRequest.newBuilder(URI("http://localhost:$port/api/v1$path"))
        if(token!=null) builder.header("Authorization","Bearer $token")
        if(body!=null) builder.header("Content-Type","application/json")
        return client.send(builder.method(method,body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString())
    }
    private fun token(user: String): String {
        val res=request("POST","/auth/login",body="""{"usuario":"$user","contrasena":"Agro2026*"}""")
        assertEquals(200,res.statusCode(),res.body()); return mapper.readTree(res.body()).path("token").asString()
    }
    @Test fun `seguridad distingue credenciales token ausente alterado y privilegios`() {
        assertEquals(401,request("GET","/lotes").statusCode())
        assertEquals(401,request("POST","/auth/login",body="""{"usuario":"tecnico1","contrasena":"error"}""").statusCode())
        val t=token("tecnico1")
        assertEquals(401,request("GET","/lotes",t+"alterado").statusCode())
        assertEquals(403,request("POST","/lotes/2/cerrar-campania",t).statusCode())
        assertEquals(200,request("GET","/usuarios/me",t).statusCode())
        assertEquals(403,request("POST","/labores",token("admin"),"{}").statusCode())
    }
    @Test fun `paginacion validacion reglas idempotencia y crud completo`() {
        val t=token("tecnico1"); val j=token("jefe1")
        val listado=request("GET","/lotes?pagina=0&tamanio=3",t)
        assertEquals(200,listado.statusCode()); assertEquals(8,mapper.readTree(listado.body()).path("totalElementos").asInt())
        assertEquals(409,request("POST","/lotes/2/cerrar-campania",j).statusCode())
        fun body(lote: Int,cantidad: Int,tipo: String="RIEGO",cliente: String="") =
            """{"loteId":$lote,"tipo":"$tipo","fecha":"2026-10-27","cantidad":$cantidad,"observacion":"Prueba","clienteId":"$cliente"}"""
        assertEquals(409,request("POST","/labores",t,body(7,1)).statusCode())
        val invalido=request("POST","/labores",t,body(2,999,"FUMIGACION"))
        assertEquals(400,invalido.statusCode()); assertTrue(mapper.readTree(invalido.body()).path("validationErrors").has("cantidad"))
        val created=request("POST","/labores",t,body(2,1,cliente="prueba-idempotencia"))
        assertEquals(201,created.statusCode(),created.body()); val id=mapper.readTree(created.body()).path("id").asLong()
        assertEquals(id,mapper.readTree(request("POST","/labores",t,body(2,1,cliente="prueba-idempotencia")).body()).path("id").asLong())
        assertEquals(200,request("PUT","/labores/$id",t,body(2,2)).statusCode())
        // Una labor del jefe no puede corregirse con el token técnico.
        val otra=request("POST","/labores",j,body(2,1))
        val otraId=mapper.readTree(otra.body()).path("id").asLong()
        assertEquals(403,request("PUT","/labores/$otraId",t,body(2,1)).statusCode())
        assertEquals(403,request("DELETE","/labores/$id",t).statusCode())
        val deleted=request("DELETE","/labores/$id",j)
        assertEquals(204,deleted.statusCode()); assertEquals("",deleted.body())
        assertEquals(404,request("PUT","/labores/$id",j,body(2,1)).statusCode())
        request("DELETE","/labores/$otraId",j)
    }
}
