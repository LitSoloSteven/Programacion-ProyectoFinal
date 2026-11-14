package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import java.sql.SQLException;
import java.util.*;
public class InquilinoService {
 private final HibernateCrudService db;
 public InquilinoService(){this(new HibernateTransactions(HibernateUtil.getSessionFactory()));}
 public InquilinoService(HibernateTransactions transactions){this.db=new HibernateCrudService(Objects.requireNonNull(transactions));}
 public List<Inquilino> listarTodos()throws SQLException{return db.listar(Inquilino.class,"from Inquilino order by nombre");}
 public List<Inquilino> listarActivos()throws SQLException{return db.listar(Inquilino.class,"from Inquilino where activo=true order by nombre");}
 public void guardar(Inquilino i)throws SQLException{if(i==null||i.getNombre()==null||i.getNombre().isBlank())throw new IllegalArgumentException("El nombre es obligatorio.");db.guardar(i,Inquilino::getId,x->{});}
 public void actualizar(UUID id,long version,String nombre,String identificacion,String telefono,String correo,String contacto,boolean activo)throws SQLException{
  db.cambiar(Inquilino.class,id,i->{
   if(i.getVersion()!=version)throw new ConflictoDatosException("Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
   i.setNombre(nombre);
   i.setIdentificacion(identificacion);
   i.setTelefono(telefono);
   i.setCorreo(correo);
   i.setContactoEmergencia(contacto);
   i.setActivo(activo);
  });
 }
 public void cambiarEstado(UUID id,boolean activo)throws SQLException{db.cambiar(Inquilino.class,id,i->i.setActivo(activo));}
 public void darDeBaja(UUID id)throws SQLException{cambiarEstado(id,false);}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Inquilino.class,id,Permiso.GESTIONAR_USUARIOS);}
}