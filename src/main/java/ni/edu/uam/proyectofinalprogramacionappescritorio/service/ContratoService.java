package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
public class ContratoService {
 private final HibernateCrudService db;
 public ContratoService(){this(new HibernateTransactions(HibernateUtil.getSessionFactory()));}
 public ContratoService(HibernateTransactions transactions){this.db=new HibernateCrudService(Objects.requireNonNull(transactions));}
 public List<Contrato> listarTodos()throws SQLException{return db.listar(Contrato.class,"from Contrato c join fetch c.unidad join fetch c.inquilino left join fetch c.garantiaSeguimiento order by c.inicio desc");}
 public List<Contrato> listarVigentes()throws SQLException{return db.listar(Contrato.class,"from Contrato c join fetch c.unidad join fetch c.inquilino left join fetch c.garantiaSeguimiento where c.estado='Vigente' order by c.inicio desc");}
 public void guardar(Contrato c)throws SQLException{if(c==null||c.getUnidadId()==null||c.getInquilinoId()==null||c.getInicio()==null||c.getFin()==null||!c.getFin().isAfter(c.getInicio()))throw new IllegalArgumentException("Verifique unidad, inquilino y fechas del contrato.");if(c.getId()==null)c.inicializarGarantiaSeguimiento();db.guardar(c,Contrato::getId,x->{});}
 public void actualizar(UUID id,long version,UUID unidadId,UUID inquilinoId,LocalDate inicio,LocalDate fin,BigDecimal canon,String moneda,int diaPago,BigDecimal deposito,String estado)throws SQLException{
  db.cambiar(Contrato.class,id,c->{
   if(c.getVersion()!=version)throw new ConflictoDatosException("Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
   c.setUnidadId(unidadId);
   c.setInquilinoId(inquilinoId);
   c.setInicio(inicio);
   c.setFin(fin);
   c.setCanon(canon);
   c.setMoneda(moneda);
   c.setDiaPago(diaPago);
   c.setDepositoPactado(deposito);
   if("Terminado".equals(estado))c.terminar(LocalDate.now());else c.setEstado(estado);
  });
 }
 public void cambiarEstado(UUID id,String estado)throws SQLException{db.cambiar(Contrato.class,id,c->{if("Terminado".equals(estado))c.terminar(LocalDate.now());else c.setEstado(estado);});}
 public void terminarContrato(UUID id)throws SQLException{cambiarEstado(id,"Terminado");}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Contrato.class,id,Permiso.GESTIONAR_USUARIOS);}
}