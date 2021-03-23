package br.com.sankhya.utils;

public class Exceptions {
	
	private String error;
	
	public Exceptions() {
		
	}
	
	public Exceptions(String error) {
		super();
		this.error = error;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String validError(String input) {

		String error = input.substring(0, 9);

		switch (error) {
		case "ORA-02291":
			return "Registro com essa chave prim�ria n�o foi encontrado.\n";
		case "ORA-00001":
			return "Registro com essa(s) chave-prim�ria(s) j� existe.\n";
		default:
			return error;
		}
	}
}
