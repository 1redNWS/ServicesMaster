package br.upf.ads.ws.classes;

public class Contagem {

	private String nome;
	private int numero;

	// Construtores
	public Contagem() {
		super();
	}

	public Contagem(String nome, int numero) {
		super();
		this.nome = nome;
		this.numero = numero;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}

}
