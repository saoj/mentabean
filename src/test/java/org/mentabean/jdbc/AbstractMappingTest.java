package org.mentabean.jdbc;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.mentabean.BeanConfig;
import org.mentabean.BeanManager;
import org.mentabean.BeanSession;
import org.mentabean.DBTypes;
import org.mentabean.util.OrderBy;
import org.mentabean.util.PropertiesProxy;
import org.mentabean.util.SQLUtils;

public class AbstractMappingTest extends AbstractBeanSessionTest {

	public static abstract class Conta {
		
		private int id;
		
		public Conta(int id) {
			this.id = id;
		}
		
		public Conta() {}
		
		public abstract String realizaOperacao();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}
	
	public static class ContaReceber extends Conta {
		
		public ContaReceber() {}
		
		public ContaReceber(int id) {
			super(id);
		}

		@Override
		public String realizaOperacao() {
			return "Conta a Receber";
		}
		
	}
	
	public static class ContaPagar extends Conta {
		
		public ContaPagar() {}
		
		public ContaPagar(int id) {
			super(id);
		}

		@Override
		public String realizaOperacao() {
			return "Conta a Pagar";
		}
		
	}
	
	public static abstract class Parcela {
		
		private int numero;
		private double valor;
		private Conta conta;
		
		public int getNumero() {
			return numero;
		}
		public void setNumero(int numero) {
			this.numero = numero;
		}
		public double getValor() {
			return valor;
		}
		public void setValor(double valor) {
			this.valor = valor;
		}
		public Conta getConta() {
			return conta;
		}
		public void setConta(Conta conta) {
			this.conta = conta;
		}
		
		public Parcela conta(Conta conta) {
			this.conta = conta;
			return this;
		}
		
		public Parcela numero(int numero) {
			this.numero = numero;
			return this;
		}
		
		public Parcela valor(double valor) {
			this.valor = valor;
			return this;
		}
		
	}
	
	public static class ParcelaReceber extends Parcela {
		
	}
	
	public static class ParcelaPagar extends Parcela {
		
	}
	
	public BeanManager configure() {
		
		BeanManager manager = new BeanManager();
		
		ParcelaPagar ppPxy = PropertiesProxy.create(ParcelaPagar.class);
		BeanConfig ppCfg = new BeanConfig(ParcelaPagar.class, "parcelas_pagar")
		.pk(ppPxy.getNumero(), DBTypes.INTEGER)
		.field(ppPxy.getConta().getId(), "idcontas_pagar", DBTypes.INTEGER)
		.field(ppPxy.getValor(), DBTypes.DOUBLE)
		.abstractInstance(ppPxy.getConta(), ContaPagar.class);
		
		ParcelaReceber prPxy = PropertiesProxy.create(ParcelaReceber.class);
		BeanConfig prCfg = new BeanConfig(ParcelaReceber.class, "parcelas_receber")
		.pk(prPxy.getNumero(), DBTypes.INTEGER)
		.field(prPxy.getConta().getId(), "idcontas_receber", DBTypes.INTEGER)
		.field(prPxy.getValor(), DBTypes.DOUBLE)
		.abstractInstance(prPxy.getConta(), ContaReceber.class)
		;
		
		manager.addBeanConfig(prCfg);
		manager.addBeanConfig(ppCfg);
		return manager;
	}
	
	@Test
	public void test() {
		
		BeanSession session = new H2BeanSession(configure(), getConnection());
		session.createTables();
		
		Parcela pr = new ParcelaReceber()
			.numero(1)
			.conta(new ContaReceber(1))
			.valor(300);
		
		session.insert(pr);
		
		Parcela prDB = new ParcelaReceber().numero(1);
		session.load(prDB);
		
		Assert.assertNotNull(prDB.getConta());
		Assert.assertTrue(prDB.getConta().realizaOperacao().equals("Conta a Receber"));
		Assert.assertEquals(ContaReceber.class, prDB.getConta().getClass());
		Assert.assertEquals(pr.getNumero(), prDB.getNumero());
		
		prDB.getConta().setId(4);
		session.update(prDB);
		
		prDB = new ParcelaReceber().numero(1);
		session.load(prDB);
		Assert.assertEquals(4, prDB.getConta().getId());
		
		ContaPagar cp = new ContaPagar(20);
		
		Parcela pp1 = new ParcelaPagar()
			.numero(1)
			.conta(cp)
			.valor(100);
		Parcela pp2 = new ParcelaPagar()
			.numero(2)
			.conta(cp)
			.valor(150);
		Parcela pp3 = new ParcelaPagar()
			.numero(3)
			.conta(cp)
			.valor(175);
		
		session.insert(pp1);
		session.insert(pp2);
		session.insert(pp3);
		
		Parcela ppProto = new ParcelaPagar().conta(cp);
		List<Parcela> list = session.loadList(ppProto, new OrderBy().orderByAsc("numero"));
		
		Assert.assertEquals(3, list.size());
		Assert.assertEquals(1, list.get(0).getNumero());
		Assert.assertEquals(2, list.get(1).getNumero());
		Assert.assertEquals(3, list.get(2).getNumero());
		Assert.assertEquals(175d, list.get(2).getValor());
		Assert.assertNotNull(list.get(2).getConta());
		
		SQLUtils.close(session.getConnection());
	}
	
}
