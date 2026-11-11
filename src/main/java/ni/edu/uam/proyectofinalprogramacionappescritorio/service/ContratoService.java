package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import java.sql.SQLException; import java.time.LocalDate; import java.util.*;
public class ContratoService {
 private final HibernateCrudService db=new HibernateCrudService();
 public List<Contrato> listarTodos()throws SQLException{return db.listar(Contrato.class,"from Contrato c join fetch c.unidad join fetch c.inquilino left join fetch c.garantiaSeguimiento order by c.inicio desc");}
 public List<Contrato> listarVigentes()throws SQLException{return db.listar(Contrato.class,"from Contrato c join fetch c.unidad join fetch c.inquilino left join fetch c.garantiaSeguimiento where c.estado='Vigente' order by c.inicio desc");}
 public void guardar(Contrato c)throws SQLException{if(c==null||c.getUnidadId()==null||c.getInquilinoId()==null||c.getInicio()==null||c.getFin()==null||!c.getFin().isAfter(c.getInicio()))throw new IllegalArgumentException("Verifique unidad, inquilino y fechas del contrato.");if(c.getId()==null)c.inicializarGarantiaSeguimiento();db.guardar(c,Contrato::getId,x->{});}
 public void terminarContrato(UUID id)throws SQLException{db.cambiar(Contrato.class,id,c->c.terminar(LocalDate.now()));}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Contrato.class,id,Permiso.GESTIONAR_USUARIOS);}
}
