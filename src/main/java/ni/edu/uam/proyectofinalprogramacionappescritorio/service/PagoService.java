package ni.edu.uam.proyectofinalprogramacionappescritorio.service;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import java.sql.SQLException; import java.util.*;
public class PagoService {
 private final HibernateCrudService db=new HibernateCrudService();
 private final HibernateTransactions tx=new HibernateTransactions(HibernateUtil.getSessionFactory());
 public List<Pago> listarTodos()throws SQLException{return db.listar(Pago.class,"from Pago order by fechaPago desc");}
 public void registrarPago(Pago p)throws SQLException{if(p==null||p.getContratoId()==null)throw new IllegalArgumentException("Seleccione un contrato.");tx.ejecutar(s->{new PermisoService().exigir(s,SesionUsuario.getActual(),Permiso.OPERAR_ALQUILERES);Contrato c=s.find(Contrato.class,p.getContratoId());if(c==null)throw new IllegalArgumentException("El contrato ya no existe.");if(!c.getMoneda().equals(p.getMonedaOriginal())){TipoCambio t=s.createQuery("from TipoCambio where fecha=:fecha and autorizadoPor is not null order by registradoEn desc",TipoCambio.class).setParameter("fecha",p.getFechaPago()).setMaxResults(1).uniqueResultOptional().orElseThrow(()->new IllegalStateException("No hay tasa autorizada para esa fecha."));p.aplicarConversion(c.getMoneda(),t.convertir(p.getMontoOriginal(),Moneda.desde(p.getMonedaOriginal()),Moneda.desde(c.getMoneda()),p.getFechaPago()),t);}else p.aplicarConversion(c.getMoneda(),p.getMontoOriginal(),null);s.persist(p);s.persist(new Recibo(p,c.toString()));return null;});}
 public void anular(UUID id)throws SQLException{db.cambiar(Pago.class,id,p->p.setEstado("Anulado"));}
 public void eliminar(UUID id)throws SQLException{db.eliminar(Pago.class,id,Permiso.GESTIONAR_USUARIOS);}
}