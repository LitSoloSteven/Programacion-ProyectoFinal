package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import java.sql.SQLException; import java.util.*;
public class UnidadService {
 private final HibernateCrudService db=new HibernateCrudService();
 public List<Unidad> listarTodas()throws SQLException{return db.listar(Unidad.class,"from Unidad order by codigo");}
 public List<Unidad> listarDisponibles()throws SQLException{return db.listar(Unidad.class,"from Unidad where estado='Disponible' order by codigo");}
 public void guardar(Unidad u)throws SQLException{if(u==null||u.getPropiedadId()==null||u.getCodigo()==null||u.getCodigo().isBlank())throw new IllegalArgumentException("Seleccione propiedad e ingrese el código.");if(u.getDescripcion()!=null&&u.getDescripcion().length()>255)throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");db.guardar(u,Unidad::getId,x->{});}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Unidad.class,id,Permiso.GESTIONAR_USUARIOS);}
}
