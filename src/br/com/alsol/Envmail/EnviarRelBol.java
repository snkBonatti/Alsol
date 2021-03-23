package br.com.alsol.Envmail;
import java.math.BigDecimal;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.comercial.BoletoHelper;
import br.com.sankhya.modelcore.util.ArquivoModeloUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

public class EnviarRelBol implements ScheduledAction { 
	
	public void onTime(ScheduledActionContext arg0) {
		
		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
        NativeSql nativeSql = new NativeSql(JDBC);
        
        StringBuilder sql = new StringBuilder();
		
        sql.append("SELECT CAB.NUNOTA , CON.NUMCONTRATO, CON.CODCONTATO, CTT.EMAIL, CAB.NUMNOTA, PAR.RAZAOSOCIAL, "
        		+ " (SELECT CONTEUDO FROM TSIATA A WHERE A.CODATA = CAB.NUNOTA AND A.TIPO = 'N' AND A.TIPOCONTEUDO = 'P' "
        		+ "  AND DTALTER = (SELECT MAX(DTALTER) FROM TSIATA WHERE CODATA = A.CODATA AND TIPO = 'N' AND TIPOCONTEUDO = 'P') "
        		+ " ) AS CONTEUDO, "
        		+ " (SELECT COUNT(*) FROM TSIATA  WHERE CODATA = CAB.NUNOTA AND TIPO = 'N' AND TIPOCONTEUDO = 'P') AS COUNT, "
        		+ " (SELECT MAX(CODFILA)+1 FROM TMDFMG) AS ULTCOD, "
        		+ " (SELECT MAX(NUANEXO)+1 FROM TMDAMG) AS ULTANEXO, "
        		+ " (SELECT MAX(NUFIN) FROM TGFFIN WHERE NUNOTA = CAB.NUNOTA) AS NUFIN, "
        		+ "GETDATE() AS DATA "
                + "  FROM TGFCAB CAB, TCSCON CON, TGFCTT CTT, TGFFIN FIN, TGFPAR PAR " 
        		+ " WHERE CAB.NUMCONTRATO = CON.NUMCONTRATO " 
                + "   AND CON.CODPARC = CTT.CODPARC " 
        		+ "	  AND CON.CODCONTATO = CTT.CODCONTATO " 
                + "   AND CAB.NUNOTA = FIN.NUNOTA "
        		+ "   AND CAB.CODPARC = PAR.CODPARC "
                + "   AND CAB.STATUSNOTA = 'L' " 
        		+ "   AND CAB.CODTIPOPER = 2122 " 
                + "   AND FIN.DHBAIXA IS NULL "
        		+ "   AND CTT.EMAIL IS NOT NULL "
         //       + "   AND CAB.NUNOTA = 170329 "
                + "	  AND ISNULL(AD_FATBOLENV,'N') = 'N' ");
        
        ResultSet rs;
		try {
			rs = nativeSql.executeQuery(sql.toString());
			
			while (rs.next()) {
				
			// DENTRO DO LOOP 	
				
				String email = rs.getString("EMAIL");
				BigDecimal nunota =  rs.getBigDecimal("NUNOTA");
				BigDecimal numnota =  rs.getBigDecimal("NUMNOTA");
				BigDecimal ultCod = rs.getBigDecimal("ULTCOD");
				BigDecimal nufin = rs.getBigDecimal("NUFIN");
				BigDecimal nuAnexo = rs.getBigDecimal("ULTANEXO");
				BigDecimal nuAnexo3 = rs.getBigDecimal("ULTANEXO");
				BigDecimal nuAnexo4 = rs.getBigDecimal("ULTANEXO");				
				Date dataatual = rs.getDate("DATA");
				String cliente = rs.getString("RAZAOSOCIAL");
				int count = rs.getInt("COUNT");
				
				
				System.out.println("NUNOTA: " + rs.getBigDecimal("NUNOTA"));
				System.out.println("EMAIL: " + email);
				
				// relatorio formatado (fatura)
				
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
				
				byte[] arqFatura = null;
				
				BigDecimal nroRelatorio = new BigDecimal(116);
				Map<String, Object> parameters = new HashMap<String, Object>();
				Report modeloImpressao = null;
				JasperPrint jasperPrint = null;
				
				parameters.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());
				parameters.put("NUNOTA", nunota);
							
				modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);
				
				jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());
				
				byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
				
				//arqFatura = JasperExportManager.exportReportToPdf(jasperPrint);
					
				
				// geração do boleto
				  BoletoHelper.ConfiguracaoBoleto cfg = new BoletoHelper.ConfiguracaoBoleto();
				     cfg.setGerarNumeroBoleto(true);
				     cfg.setUsaContaBcoFinanceiros(true);
				     cfg.setFinanceirosSelecionados(Arrays.asList(new BigDecimal[] { nufin }));
				     cfg.setTipoSaidaBoleto(1);

				BoletoHelper boletoHelper = new BoletoHelper();

				boletoHelper.gerarBoleto(cfg);

				byte[] teste123 = boletoHelper.getBoletosPDF();
								
				
				// Geração do e-mail
				

				
				String corpoemail = "<html>" 
				        + "<head>" 
						+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" /> " 
				        + "<title>Untitled Document</title> " 
						+ "</head>" 
				        + "<body>" 
						+ "<p> Prezado Cliente, <strong> " 
				        + cliente 
				        + "</strong></p> " 
				        + "<p>Encaminhamos em anexo fatura de loca&ccedil;&atilde;o, boleto e relat&oacute;rio de compensa&ccedil;&atilde;o de cr&eacute;ditos.<u></u><u></u></p>" 
				        + "<p>Qualquer d&uacute;vida entre em contato com a nossa equipe de p&oacute;s-venda:<u></u><u></u></p> " 
				        + "<p><u></u>&nbsp;<u></u></p>" 
				        + "<p>(34) 2512-8820 / (34) 99838-8044 <img src=\"http://s2.glbimg.com/3Qhnv2I3Rr2rtpcdR0_9LurZVbc=/695x0/s.glbimg.com/po/tt2/f/original/2016/05/12/whatsapp-logo.jpg\" width=\"69\" height=\"34\"></p>" 
				        + "<p><u></u>&nbsp;<u></u></p>" 
				        + "<p>Estamos &agrave; disposi&ccedil;&atilde;o.<u></u><u></u></p>" 
				        + "<p><u></u>&nbsp;<u></u></p> " 
				        + "<p><strong>Depto Cobran&ccedil;a</strong><u></u><u></u></p> " 
				        + "<p>34&nbsp;2512-8813<u></u><u></u></p>" 
				        + "<p><a href=\"mailto:cobranca@alsolenergia.com.br\" target=\"_blank\">cobranca@alsolenergia.com.br</a><u></u><u></u></p>" 
				        + "<p><u></u>&nbsp;<a href=\"http://alsolenergia.com.br/\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?q=http://alsolenergia.com.br&source=gmail&ust=1582313709353000&usg=AFQjCNFi0HtRBfWt5dqIpxGknXH03ts_GA\">alsolenergia.com.br</a><u></u><u></u></p>" 
				        + "<p><u></u>&nbsp;<u></u><img src= \"http://alsolenergia.com.br/wp-content/themes/alsol/images/logo-alsol.png\" width=\"206\" height=\"65\"> <img src=\"http://alsolenergia.com.br/wp-content/themes/alsol/images/logo-grupo-energisa.png\" width=\"206\" height=\"65\"></p>"
				        + "<p><u></u>&nbsp;<u></u></p>"
				        + "<div>" 
				        + " <p>Att.</p>" 
				        + "</div>" 
				        + "</body>" 
				        + "</html>" ;
								
				char[] mensagem = corpoemail.toCharArray();
				
				String assunto = "Fatura";
				
				SessionHandle hnd = null;
				try {
					hnd = JapeSession.open();
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
					DynamicVO dynamicVO = (DynamicVO) entityVO;
					//dynamicVO.setProperty("CODFILA", ultCod);
					dynamicVO.setProperty("ASSUNTO", assunto);
					dynamicVO.setProperty("DTENTRADA", dataatual);
					dynamicVO.setProperty("STATUS", "Pendente");
					dynamicVO.setProperty("EMAIL", email);
					dynamicVO.setProperty("TENTENVIO", new BigDecimal(1) );
					dynamicVO.setProperty("MENSAGEM", mensagem );
					dynamicVO.setProperty("NUCHAVE", nunota );
					dynamicVO.setProperty("TIPOENVIO", "E");
					dynamicVO.setProperty("MAXTENTENVIO", new BigDecimal(3) );			
					dynamicVO.setProperty("CODCON", new BigDecimal(0));		
					
					PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", entityVO);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					
					BigDecimal codFila = save.asBigDecimal("CODFILA");
					
					ultCod = codFila;
					
					System.out.println("nro fila: " + codFila);
					
				} finally {
					JapeSession.close(hnd);
				}
				
				
				// criar anexo 1 Boleto
				
				SessionHandle anexo2 = null;
				try {
					anexo2 = JapeSession.open();
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
					DynamicVO dynamicVO = (DynamicVO) entityVO;
					dynamicVO.setProperty("NOMEARQUIVO", "BOLETO");
					dynamicVO.setProperty("TIPO", "application/pdf");
					dynamicVO.setProperty("ANEXO", teste123);
						
					PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					
					BigDecimal codAnexo = save.asBigDecimal("NUANEXO");
					
					nuAnexo = codAnexo;
					
					System.out.println("nro ANEXO: " + codAnexo);
					
				} finally {
					JapeSession.close(anexo2);
				}

				// anexo 2 Fatura
				SessionHandle anexo3 = null;
				try {
					anexo3 = JapeSession.open();
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
					DynamicVO dynamicVO = (DynamicVO) entityVO;
					dynamicVO.setProperty("NOMEARQUIVO", "Fatura: "+ numnota);
					dynamicVO.setProperty("TIPO", "application/pdf");
					dynamicVO.setProperty("ANEXO", pdf);
						
					PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					
					BigDecimal codAnexo3 = save.asBigDecimal("NUANEXO");
					
					nuAnexo3 = codAnexo3;
					
					System.out.println("nro ANEXO 3: " + codAnexo3);
					
				} finally {
					JapeSession.close(anexo3);
				}
				
				// anexo 3 Rel. Produção
				
				if (count != 0)
						
						{
				
				SessionHandle anexo4 = null;
				try {
					anexo4 = JapeSession.open();
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
					DynamicVO dynamicVO = (DynamicVO) entityVO;
					dynamicVO.setProperty("NOMEARQUIVO", "Produção");
					dynamicVO.setProperty("TIPO", "application/pdf");
					dynamicVO.setProperty("ANEXO", arqFatura);
						
					PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					
					BigDecimal codAnexo4 = save.asBigDecimal("NUANEXO");
					
					nuAnexo4 = codAnexo4;
					
					System.out.println("nro ANEXO 4: " + codAnexo4);
					
				} finally {
					JapeSession.close(anexo4);
				}
				}
				
				String sqlAnexo = " INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES "
						+ "(" + ultCod + " , " + nuAnexo + ")";
				
				String sqlAnexo3 = " INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES "
						+ "(" + ultCod + " , " + nuAnexo3 + ")";
				
				String sqlAnexo4 = " INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES "
						+ "(" + ultCod + " , " + nuAnexo4 + ")";

				String sqlUpdateFim = "UPDATE TGFCAB SET AD_FATBOLENV = 'S' WHERE NUNOTA = "+ nunota;
	
				String sqlConteudo = "UPDATE TMDAMG SET ANEXO = (SELECT A.CONTEUDO FROM TSIATA A WHERE A.CODATA = " 
						+ nunota 
						+ " AND A.TIPO = 'N' AND DTALTER = (SELECT MAX(DTALTER) FROM TSIATA WHERE CODATA = A.CODATA AND TIPO = 'N')) " 
						+"	WHERE NUANEXO = " 
						+ nuAnexo4;
				
					
				// Insere o anexo
				nativeSql.executeUpdate(sqlAnexo);	
				
				// Insere o anexo 3
				nativeSql.executeUpdate(sqlAnexo3);	
				
				if (count != 0)
					
				{
				// Insere o anexo 4
				nativeSql.executeUpdate(sqlAnexo4);	
				
				
				// Insere o anexo 4
				nativeSql.executeUpdate(sqlConteudo);	
				
				}
				
				// executa 
				nativeSql.executeUpdate(sqlUpdateFim);	
			
			
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
