package br.com.alsol.importxls;

import java.io.File;
//import java.text.ParseException;
//import java.math.BigDecimal;
//import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import jxl.Sheet;
import jxl.Workbook;

public class ImportXls implements AcaoRotinaJava {

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

		String msgRetorn = "Arquivo processado! <br><br>", fileInput = "", nomeArquivo = "";

		QueryExecutor queryB = ca.getQuery();
		String nuImport = line.getCampo("NROUNICO").toString();

		int error = 0;

		try {

			queryImport.update("DELETE AD_IMPCONITE WHERE NROUNICO = " + nuImport);

			queryB.nativeSelect("SELECT CHAVEARQUIVO, NOMEARQUIVO FROM TSIANX WHERE NOMEINSTANCIA = 'AD_IMPCONSUMO'");

			if (queryB.next()) {

				fileInput = queryB.getString("CHAVEARQUIVO");
				nomeArquivo = queryB.getString("NOMEARQUIVO");

				Workbook workbook = Workbook
						.getWorkbook(new File("/home/algaralsol/Sistema/Anexos/AD_IMPCONSUMO/" + fileInput));

				Sheet sheet = workbook.getSheet(0);

				int linhas = sheet.getRows();

				for (int i = 1; i < linhas; i++) {

					try {

						queryImport.update("INSERT INTO AD_IMPCONITE " + "(NROUNICO, " + "SEQUENCIA, " + "DHIMP, "
								+ "NOMEARQ, " + "NROINSTALACAO, " + "CNPJ, " + "RAZAOSOCIAL, " + "DIAVENC, "
								+ "CREDCOMPEN, " + "VLRTOT, " + "GERADO, "+ "CR ) VALUES " + "(" + nuImport + ", " + (i + 1)
								+ ", getdate() , '" + nomeArquivo + "' , "
								+ validInput((sheet.getCell(0, i)).getContents()) + " , "
								+ validCnpj((sheet.getCell(1, i)).getContents()) + " , "
								+ (validInput((sheet.getCell(2, i)).getContents()).length() > 95
										? validInput((sheet.getCell(2, i)).getContents().substring(0, 90))
										: validInput((sheet.getCell(2, i)).getContents()))
								+ " , " + validInput((sheet.getCell(3, i)).getContents()) + " , "
								+ validInput((sheet.getCell(4, i)).getContents()) + " , "
								+ validInput((sheet.getCell(5, i)).getContents()) + ", 'N', "
								+ validInput((sheet.getCell(6, i)).getContents())+" )");

					} catch (Exception e1) {

						queryImport.update("INSERT INTO AD_IMPCONALE (NROUNICO, SEQUENCIA, ALERTA) VALUES (" + nuImport
								+ ", " + (i + 1) + ", '" + e1.getMessage() + "')");
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
			msgRetorn += "\nForam encontrados erros. Verifique a aba de Alertas para visualizar mais detalhes.";
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
			} else {
				return "'" + (input.replace(".", "")).replace(",", ".").replace("'", "") + "'";
			}
		}
	}

	private String validCnpj(String input) {

		String cnpj = "NULL";

		switch (input) {
		case "":
			cnpj = "NULL";
			break;
		default:
			if (input.trim().length() == 13) {
				cnpj = "'0" + (input.replace(".", "")).replace(",", ".").replace("'", "") + "'";
			} else if (input.trim().length() == 12) {
				cnpj = "'00" + (input.replace(".", "")).replace(",", ".").replace("'", "") + "'";
			} else {
				cnpj = "'" + (input.replace(".", "")).replace(",", ".").replace("'", "") + "'";
			}
		}

		return cnpj;
	}

	public static boolean verifyDateHour(String qqString) {
		return qqString.contains("/") && qqString.contains(":");
	}

}