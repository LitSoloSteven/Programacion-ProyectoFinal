package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
public class UnidadService {
 private final HibernateCrudService db;
 public UnidadService(){this(new HibernateTransactions(HibernateUtil.getSessionFactory()));}
 public UnidadService(HibernateTransactions transactions){this.db=new HibernateCrudService(Objects.requireNonNull(transactions));}
 public List<Unidad> listarTodas()throws SQLException{return db.listar(Unidad.class,"from Unidad order by codigo");}
 public List<Unidad> listarDisponibles()throws SQLException{return db.listar(Unidad.class,"from Unidad where estado='Disponible' order by codigo");}
 public void guardar(Unidad u)throws SQLException{if(u==null||u.getPropiedadId()==null||u.getCodigo()==null||u.getCodigo().isBlank())throw new IllegalArgumentException("Seleccione propiedad e ingrese el código.");if(u.getDescripcion()!=null&&u.getDescripcion().length()>255)throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");db.guardar(u,Unidad::getId,x->{});}
 public void actualizar(UUID id,long version,UUID propiedadId,String codigo,String descripcion,String tipo,String estado,BigDecimal canon,String moneda)throws SQLException{
  db.cambiar(Unidad.class,id,u->{
   if(u.getVersion()!=version)throw new ConflictoDatosException("Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
   u.setPropiedadId(propiedadId);
   u.setCodigo(codigo);
   u.setDescripcion(descripcion);
   u.setTipo(tipo);
   u.setEstado(estado);
   u.setCanonReferencia(canon);
   u.setMoneda(moneda);
  });
 }
 public void cambiarEstado(UUID id,String estado)throws SQLException{db.cambiar(Unidad.class,id,u->u.setEstado(estado));}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Unidad.class,id,Permiso.GESTIONAR_USUARIOS);}
}