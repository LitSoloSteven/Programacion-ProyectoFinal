package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import java.sql.SQLException;
import java.util.*;
public class PropiedadService {
 private final HibernateCrudService db=new HibernateCrudService();
 private final HibernateTransactions transactions=new HibernateTransactions(HibernateUtil.getSessionFactory());
 public List<Propiedad> listarTodas() throws SQLException{return db.listar(Propiedad.class,"from Propiedad p left join fetch p.propietario order by p.nombre");}
 public List<Propiedad> listarActivas() throws SQLException{return db.listar(Propiedad.class,"from Propiedad p left join fetch p.propietario where p.activa=true order by p.nombre");}
 public void guardar(Propiedad p, String nombrePropietario, String correoPropietario, String telefonoPropietario)throws SQLException{
  if(p==null||p.getNombre()==null||p.getNombre().isBlank()||p.getDireccion()==null||p.getDireccion().isBlank())throw new IllegalArgumentException("Nombre y dirección son obligatorios.");
  if(nombrePropietario==null||nombrePropietario.isBlank()||nombrePropietario.strip().length()>100
    ||correoPropietario==null||correoPropietario.strip().length()>150||!correoPropietario.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
    ||telefonoPropietario!=null&&telefonoPropietario.strip().length()>20)
   throw new IllegalArgumentException("Ingrese un nombre (máximo 100 caracteres), un correo válido (máximo 150) y un teléfono de hasta 20 caracteres.");
  SesionUsuario sesion=SesionUsuario.getActual();
  if(sesion==null)throw new SecurityException("Inicie sesión para guardar una propiedad.");
  transactions.ejecutar(s->{
   Usuario usuario=new PermisoService().exigir(s,sesion,Permiso.OPERAR_ALQUILERES);
   Propietario propietario=s.find(Propietario.class,usuario.getId());
   if(propietario==null){
    propietario=new Propietario(usuario,nombrePropietario.strip(),correoPropietario.strip().toLowerCase(Locale.ROOT),limpiarTelefono(telefonoPropietario));
    s.persist(propietario);
   }else{
    propietario.setNombre(nombrePropietario.strip());
    propietario.setCorreo(correoPropietario.strip().toLowerCase(Locale.ROOT));
    propietario.setTelefono(limpiarTelefono(telefonoPropietario));
   }
   if(p.getId()==null){
    p.setPropietario(propietario);
    s.persist(p);
   }else{
    Propiedad existente=s.find(Propiedad.class,p.getId());
    if(existente==null)throw new IllegalArgumentException("La propiedad ya no existe.");
    existente.setNombre(p.getNombre().strip());
    existente.setDireccion(p.getDireccion().strip());
    existente.setActiva(p.isActiva());
    existente.setPropietario(propietario);
   }
   return null;
  });
 }
 private String limpiarTelefono(String telefono){return telefono==null||telefono.isBlank()?null:telefono.strip();}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Propiedad.class,id,Permiso.GESTIONAR_USUARIOS);}
 public void cambiarEstado(UUID id,boolean activa)throws SQLException{db.cambiar(Propiedad.class,id,p->p.setActiva(activa));}
}
