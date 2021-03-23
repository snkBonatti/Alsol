package br.com.alsol.Envmail;

import java.io.File;
//import java.text.ParseException;
//import java.math.BigDecimal;
//import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.utils.Exceptions;
import jxl.Sheet;
import jxl.Workbook;

public class ImportXML implements AcaoRotinaJava {

	public void doAction(ContextoAcao ca) throws Exception {
		for (int i = 0; i < ca.getLinhas().length; i++) {
			Registro line = ca.getLinhas()[i];
			try {
				
				confirmImport(ca, line);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void confirmImport(ContextoAcao ca, Registro line) {

		QueryExecutor queryImport = ca.getQuery();
		
		String msgRetorn = "Arquivo processado! <br><br>", scriptInsert = "", scriptValue = "", fileInput = "",
				nomeArquivo = "";
		
		QueryExecutor queryB = ca.getQuery();
		
		//String table = line.getCampo("NOMETAB").toString();
		
		String nuImport = line.getCampo("NROUNICO").toString();
		
		Exceptions exceptions = new Exceptions();
		int error = 0;

		try {
			
			queryImport.update("DELETE AD_IMPCONITE WHERE NROUNICO = " + nuImport);

			queryB.nativeSelect("SELECT CHAVEARQUIVO, NOMEARQUIVO FROM TSIANX WHERE NOMEINSTANCIA = 'AD_IMPCONSUMO'");

			if (queryB.next()) {

				fileInput = queryB.getString("CHAVEARQUIVO");
				nomeArquivo = queryB.getString("NOMEARQUIVO");

				Workbook workbook = Workbook
						.getWorkbook(new File("/home/algaralsol/Sistema/Anexos/AD_IMPCONSUMO/" + fileInput));
				
				System.out.println("PONTO 1");

				Sheet sheet = workbook.getSheet(0);

				int linhas = sheet.getRows();
				
				System.out.println("Linhas " + linhas );

				//int colunas = sheet.getColumns();

				for (int i = 3; i < linhas; i++) {
					
						try {
							
							System.out.println("Linha: "+ (i + 1));
							System.out.println("Dados Linha " +  validInput((sheet.getCell(0, i)).getContents()));
							
							//double custoCompens = Double.parseDouble(sheet.getCell(4, i).toString());
							//double valorTotal = Double.parseDouble(sheet.getCell(7, i).toString());
							

							//queryImport.update(scriptInsert + scriptValue);
							queryImport.update("INSERT INTO AD_IMPCONITE (NROUNICO, SEQUENCIA, DHIMP, NOMEARQ, NROINSTALACAO, CNPJ, RAZAOSOCIAL, DIAVENC, CREDCOMPEN, VLRTOT) VALUES "
							+ "(" 
							+ nuImport 
							+ ", " 
							+ (i + 1) 
							+ ", getdate() , '" 
							+ nomeArquivo  
							+"' , " 
							+ validInput((sheet.getCell(3, i)).getContents()) + " , " 
							+ validInput((sheet.getCell(4, i)).getContents()) + " , "  
							+ validInput((sheet.getCell(5, i)).getContents()) + " , " 
							+ validInput((sheet.getCell(6, i)).getContents()) + " , "
							//+ custoCompens
							+ validInput((sheet.getCell(7, i)).getContents()) + " , "
							//+ valorTotal
							+ validInput((sheet.getCell(12, i)).getContents()) 
							+" )");

						} catch (Exception e1) {

							e1.printStackTrace();
							 msgRetorn = msgRetorn + "Erro na linha " + (i + 1) + ": " + exceptions.validError(e1.getMessage());
							//queryImport.update("INSERT INTO AD_LOGIMPORTADOR (NUIMPORT, LINHA, TIPO, DTMOV, DESCRICAO, NOMEARQUIVO) VALUES (" + nuImport + ", " + (i + 1) + ", 'E', SYSDATE, '" + exceptions.validError(e1.getMessage()) + "', '"+ nomeArquivo +  "')");
							error += 1;
						}
				}

				workbook.close();

				queryImport.update("DELETE TSIANX WHERE NOMEINSTANCIA = 'AD_IMPCONSUMO'");
				
				File pasta = new File("/home/algaralsol/Sistema/Anexos/AD_IMPCONSUMO/");
				File[] arquivos = pasta.listFiles();

				for (File arquivo : arquivos) {
					arquivo.delete();
				}
				
				queryB.close();
				queryImport.close();
				
			} else {
				
				msgRetorn = "Nenhum arquivo foi encontrado para importar!";
			}

		} catch (Exception e1) {
			
			e1.printStackTrace();
			ca.setMensagemRetorno(e1.getMessage());
		} 
		
		if (error > 0) {
			msgRetorn += "Foram encontrados erros. Verifique a aba de log para visualizar mais detalhes.";			
		}

		ca.setMensagemRetorno(msgRetorn);
	}

	private String validInput(String input) {
						
		switch (input) {
		case "":
			return "NULL";
		default:
			if (verifyDateHour(input)) {
				return "TO_DATE('" + (input.replace(".", ",")).replace("'", "") + "',  'DD/MM/YYYY HH24:MI:SS')";
			}else {
				//return "'" + (input.replace(".", ",")).replace("'", "") + "'";
				return "'" + input.replace("'", "") + "'";
			}			
		}
	}
	
	public static boolean verifyDateHour (String qqString){
	    return qqString.contains("/") && qqString.contains(":");
	}	
	
}