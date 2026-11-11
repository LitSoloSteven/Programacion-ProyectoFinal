package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import java.sql.SQLException; import java.util.*;
public class CargoService {
 private final HibernateCrudService db=new HibernateCrudService();
 public List<Cargo> listarTodos()throws SQLException{return db.listar(Cargo.class,"from Cargo order by periodo desc");}
 public void guardar(Cargo c)throws SQLException{if(c==null||c.getContratoId()==null||c.getImporte()==null)throw new IllegalArgumentException("Contrato e importe son obligatorios.");db.guardar(c,Cargo::getId,x->{});}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Cargo.class,id,Permiso.GESTIONAR_USUARIOS);}
}