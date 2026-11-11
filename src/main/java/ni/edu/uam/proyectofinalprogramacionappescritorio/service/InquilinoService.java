package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import java.sql.SQLException; import java.util.*;
public class InquilinoService {
 private final HibernateCrudService db=new HibernateCrudService();
 public List<Inquilino> listarTodos()throws SQLException{return db.listar(Inquilino.class,"from Inquilino order by nombre");}
 public List<Inquilino> listarActivos()throws SQLException{return db.listar(Inquilino.class,"from Inquilino where activo=true order by nombre");}
 public void guardar(Inquilino i)throws SQLException{if(i==null||i.getNombre()==null||i.getNombre().isBlank())throw new IllegalArgumentException("El nombre es obligatorio.");db.guardar(i,Inquilino::getId,x->{});}
 public void darDeBaja(UUID id)throws SQLException{db.cambiar(Inquilino.class,id,i->i.setActivo(false));}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Inquilino.class,id,Permiso.GESTIONAR_USUARIOS);}
}