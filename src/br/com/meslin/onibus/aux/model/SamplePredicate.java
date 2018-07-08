package br.com.meslin.onibus.aux.model;

import java.util.function.Predicate;

/**
 * This filter seaches for a bus list for a bus based on its "ordem"<br>
 * 
 * @author meslin
 *
 */
public class SamplePredicate implements Predicate<Bus>
{
	String ordem;

	/**
	 * Constrói um filtro baseado no nome do usuário
	 * @param ordem
	 */
	public SamplePredicate(String ordem)
	{
		this.ordem = ordem;
	}

	/**
	 * Verifica se o username é desse usuário
	 */
	public boolean test(Bus bus)
	{
		if (ordem.equals(bus.getOrdem())) return true;
		return false;
	}
}
