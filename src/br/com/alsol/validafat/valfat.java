package br.com.alsol.validafat;


import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import java.sql.ResultSet;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.LiberacaoSolicitada;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class valfat implements AcaoRotinaJava {

	String mensagem = " ";
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		
		for (int i = 0; i < ctx.getLinhas().length; i++) {
			Registro line = ctx.getLinhas()[i];
			try {
				
				exemplo(ctx, line);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("static-access")
	private void exemplo(ContextoAcao ctx, Registro line) {
		

		BigDecimal nunota = (BigDecimal) line.getCampo("NUNOTA");
		BigDecimal codparc = (BigDecimal) line.getCampo("CODPARC");
		BigDecimal codTipoper = (BigDecimal) line.getCampo("CODTIPOPER");
		Date data = (Date) line.getCampo("DTNEG");
		GregorianCalendar dataCal = new GregorianCalendar();
		dataCal.setTime(data);
		int mes = dataCal.get(Calendar.MONTH);
		int ano = dataCal.get(Calendar.YEAR);
		
		System.out.println("PARTE 01 THIAGO BONATTI " + mes + " ANO " + ano);
		
	     JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
	     NativeSql nativeSql = new NativeSql(JDBC);
	     StringBuilder sql = new StringBuilder();
	        
	        sql.append(" SELECT PAR.RAZAOSOCIAL, (SELECT COUNT(*) AS COUNT ");
	        sql.append("      FROM TGFCAB C1 ");
	        sql.append("   WHERE C1.NUNOTA <> " + nunota );
	        sql.append("   AND C1.CODTIPOPER = " + codTipoper);
	        sql.append("   AND C1.CODPARC = " + codparc);
	        sql.append("   AND DATEPART(MM, C1.DTNEG) = DATEPART(MM, C.DTNEG)" );
	        sql.append("   AND DATEPART(YYYY, C1.DTNEG) = " + ano);
	        sql.append("  ) AS COUNT,  ");
	        sql.append("  REPLACE(SUBSTRING(CONVERT(varchar, C.DTNEG, 103), 4, 7), '/',  '') AS PASTA, ");
	        sql.append(" LTRIM(RTRIM(PAR.CGC_CPF)) AS CGC_CPF, ");
	        sql.append(" ( SELECT COUNT(*)");
	        sql.append("      FROM TGFITE ");
	        sql.append("     WHERE AD_NROINSTALACAO IS NULL AND NUNOTA = " + nunota + ") AS COUNT2");
	        sql.append(" FROM TGFCAB C, TGFPAR PAR");
	        sql.append(" WHERE C.CODPARC = PAR.CODPARC ");
	        sql.append("   AND C.NUNOTA = " + nunota );
	        sql.append("   AND C.CODTIPOPER = " + codTipoper);
	        sql.append("   AND C.CODPARC = " + codparc);
	        sql.append(" GROUP BY PAR.RAZAOSOCIAL, REPLACE(SUBSTRING(CONVERT(varchar, C.DTNEG, 103), 4, 7), '/',  ''), LTRIM(RTRIM(PAR.CGC_CPF)), DATEPART(MM, C.DTNEG) ");
	     
			
			try {

				JDBC.openSession();
				ResultSet rs =  nativeSql.executeQuery(sql.toString());
				
				while (rs.next()) {
					
					String razao = rs.getString("RAZAOSOCIAL");
					String pasta = rs.getString("PASTA");
					String cnpj = rs.getString("CGC_CPF");
					
					System.out.println("PARTE 02 THIAGO BONATTI " + pasta);
					
					if (rs.getInt("COUNT") > 0 )
					{
						
						System.out.println("PARTE 03 THIAGO BONATTI DENTRO DO IF 1" + razao);
						
					mensagem = mensagem +  "Nro Unico " + nunota + " Parceiro: " + razao + " Possui mais de uma nota no mesmo mês! <br>";
					}
					
					if (rs.getInt("COUNT2") > 0 )
					{
						System.out.println("PARTE 04 THIAGO BONATTI DENTRO DO IF 2" + razao);
						
			     	mensagem = mensagem +  "Nro Unico " + nunota + " Parceiro: " + razao + " Possui itens sem o nro de Instalação preenchido! <br>";
					}
					
					System.out.println("PARTE 05 THIAGO BONATTI antes da validação do pdf." + razao);
					
					File path = new File("/home/algaralsol/Consumo/" + pasta + "/");
					File[] arquivos = path.listFiles(new FilenameFilter() {
						// apply a filter
						@Override
						public boolean accept(File dir, String name) {
							boolean result;
							if (name.startsWith(cnpj + ".pdf")) {
								result = true;
							} else {
								result = false;
							}
							return result;
							
						}
					});
					
					
					System.out.println("PARTE 06 THIAGO BONATTI dentro da validação do pdf " );
					
					String temPDF = "N";
					
					if (arquivos != null) {
						for (File arquivo : arquivos) {
							
							if (arquivo.getName().equals(cnpj + ".pdf")) {
								
								temPDF = "S";
								 System.out.printf("THIAGO BONATTI - Parte 07 - Entrou no if" + arquivo.getName());
								
							} 
																
								
					}
						
						 System.out.printf("THIAGO BONATTI - Parte 08 - Antes do ultimo IF " );
						
						if (temPDF == "N") {
							
							mensagem = mensagem +   "Nro Unico " + nunota + " Parceiro: " + razao + " Não possui PDF do consumo! <br>";

						}
						
						
					}
					
					
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				JdbcWrapper.closeSession(JDBC);
			}
	        
			ctx.setMensagemRetorno(mensagem);

	}
	}
	

