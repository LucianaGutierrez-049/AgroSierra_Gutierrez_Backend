import json,subprocess,urllib.request,urllib.error,time,datetime
from pathlib import Path
BASE='http://localhost:8081/api/v1'
ROOT=Path(__file__).resolve().parents[1]
resultados=[]
def sql(texto):
    r=subprocess.run(['docker','exec','-i','agrosierra-oracle','sqlplus','-s','AGRODB/AgroDb2026_Dev@//localhost:1521/FREEPDB1'],
        input='whenever sqlerror exit sql.sqlcode\nset heading off feedback off pagesize 0\n'+texto+'\nexit;\n',text=True,capture_output=True)
    if r.returncode: raise RuntimeError(r.stdout+r.stderr)
    return r.stdout.strip()
def http(method,path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=20) as r: status,data=r.status,r.read().decode()
    except urllib.error.HTTPError as e: status,data=e.code,e.read().decode()
    return status,json.loads(data) if data else None
def check(nombre,respuesta,esperado):
    status,body=respuesta
    resultados.append({'prueba':nombre,'esperado':esperado,'obtenido':status,'correcto':status==esperado})
    if status!=esperado: raise AssertionError(f'{nombre}: HTTP {status}: {body}')
    return body
def login(user):
    return check('Login '+user,http('POST','/auth/login',{'usuario':user,'contrasena':'Agro2026*'}),200)['token']
creadas=[]; lote_prueba=None
for intento in range(30):
    try:
        if http('POST','/auth/login',{'usuario':'tecnico1','contrasena':'Agro2026*'})[0]==200: break
    except (urllib.error.URLError,TimeoutError): pass
    time.sleep(2)
else: raise RuntimeError('AgroAPI no terminó de iniciar en 60 segundos; revisar data/agroapi-oracle.log')
try:
    tecnico=login('tecnico1'); jefe=login('jefe1'); admin=login('admin')
    check('Credenciales incorrectas',http('POST','/auth/login',{'usuario':'tecnico1','contrasena':'incorrecta'}),401)
    check('Token ausente',http('GET','/lotes'),401)
    check('Firma JWT alterada',http('GET','/lotes',token=tecnico+'alterado'),401)
    check('Usuario actual',http('GET','/usuarios/me',token=tecnico),200)
    listado=check('Lotes paginados',http('GET','/lotes?pagina=0&tamanio=100',token=tecnico),200)
    assert listado['totalElementos']>=8
    lotes=listado['contenido']; abierto=next(l for l in lotes if not l['campaniaCerrada']); cerrado=next(l for l in lotes if l['campaniaCerrada'])
    filtrado=check('Filtro de campaña',http('GET','/lotes?campaniaCerrada=false',token=tecnico),200)
    assert all(not l['campaniaCerrada'] for l in filtrado['contenido'])
    check('Recurso inexistente',http('GET','/lotes/999999999',token=tecnico),404)
    check('Privilegio de cierre',http('POST',f"/lotes/{abierto['id']}/cerrar-campania",token=tecnico),403)
    def labor(lote,tipo='RIEGO',cantidad=1,cliente=None):
        return {'loteId':lote,'tipo':tipo,'fecha':datetime.date.today().isoformat(),'cantidad':cantidad,'observacion':'Verificación automática Oracle','clienteId':cliente}
    check('Administrador no registra labores',http('POST','/labores',labor(abierto['id']),admin),403)
    check('RN-01 campaña cerrada',http('POST','/labores',labor(cerrado['id']),tecnico),409)
    bad=check('RN-04 dosis excesiva',http('POST','/labores',labor(abierto['id'],'FUMIGACION',999999),tecnico),400)
    assert 'cantidad' in bad['validationErrors']
    cliente='verificacion-oracle-'+str(time.time_ns())
    nueva=check('Crear labor',http('POST','/labores',labor(abierto['id'],cliente=cliente),tecnico),201); creadas.append(nueva['id'])
    repetida=check('Creación idempotente',http('POST','/labores',labor(abierto['id'],cliente=cliente),tecnico),201)
    assert nueva['id']==repetida['id']
    check('Editar labor propia',http('PUT',f"/labores/{nueva['id']}",labor(abierto['id'],cantidad=2),tecnico),200)
    sql(f"update labor set registrado_en = sys_extract_utc(systimestamp) - interval '25' hour where id = {nueva['id']};\ncommit;")
    check('RN-02 plazo de 24 horas',http('PUT',f"/labores/{nueva['id']}",labor(abierto['id']),tecnico),403)
    check('Jefe corrige fuera del plazo',http('PUT',f"/labores/{nueva['id']}",labor(abierto['id']),jefe),200)
    ajena=check('Jefe registra labor',http('POST','/labores',labor(abierto['id']),jefe),201); creadas.append(ajena['id'])
    check('RN-02 autor ajeno',http('PUT',f"/labores/{ajena['id']}",labor(abierto['id']),tecnico),403)
    check('Técnico no elimina',http('DELETE',f"/labores/{nueva['id']}",token=tecnico),403)
    codigo='QA-'+str(time.time_ns())
    sql(f"insert into lote(codigo,nombre,hectareas,cultivo,valle,campania_cerrada,dosis_maxima_por_hectarea) values('{codigo}','Lote temporal QA',2,'Papa','Mantaro',false,2.5);\ncommit;")
    lote_prueba=int(sql(f"select id from lote where codigo='{codigo}';"))
    check('RN-03 cierre sin cosecha',http('POST',f'/lotes/{lote_prueba}/cerrar-campania',token=jefe),409)
    cosecha=check('Registrar cosecha',http('POST','/labores',labor(lote_prueba,'COSECHA'),jefe),201); creadas.append(cosecha['id'])
    campania=check('Cerrar campaña con cosecha',http('POST',f'/lotes/{lote_prueba}/cerrar-campania',token=jefe),200)
    assert campania['campaniaCerrada']
    for identificador in list(creadas):
        assert check('Eliminar sin cuerpo',http('DELETE',f'/labores/{identificador}',token=jefe),204) is None
        creadas.remove(identificador)
    version=sql('select "version" || chr(32) || "success" from "flyway_schema_history" where "version" is not null;')
    conteos=sql("select 'usuarios=' || count(*) from usuario;\nselect 'lotes=' || count(*) from lote where codigo not like 'QA-%';\nselect 'labores=' || count(*) from labor;")
    print('Flyway:',version); print(conteos)
finally:
    for identificador in creadas:
        sql(f'delete from labor where id={identificador};\ncommit;')
    if lote_prueba is not None:
        sql(f'delete from labor where lote_id={lote_prueba};\ndelete from lote where id={lote_prueba};\ncommit;')
    carpeta=ROOT/'docs'; carpeta.mkdir(exist_ok=True)
    (carpeta/'verificacion-oracle.json').write_text(json.dumps(resultados,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'ORACLE: {len(resultados)} comprobaciones HTTP correctas; datos temporales eliminados.')
