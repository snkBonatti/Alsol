package br.com.alsol.send.email;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.jdom.Content;
import org.jdom.Element;

import com.sankhya.util.TimeUtils;
import com.sankhya.util.XMLUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BoletoHelper;
import br.com.sankhya.modelcore.util.ArquivoModeloUtils;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.ws.ServiceContext;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

public class SendEmail implements ScheduledAction {

	private AuthenticationInfo oldAuthInfo;
	private AuthenticationInfo authInfo;
	private final ServiceContext sctx = new ServiceContext(null);
	
	public void onTime(ScheduledActionContext arg0) {
		
		

		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	      
		NativeSql nativeSql = new NativeSql(jdbc);
		StringBuilder sql = new StringBuilder();
		
		System.out.println("THIAGO BONATTI - INICIO");

		sql.append("SELECT CAB.NUNOTA , "
				+ " REPLACE(SUBSTRING(CONVERT(varchar, CAB.DTNEG, 103), 4, 7), '/',  '') AS PASTA, "	
				+ " LTRIM(RTRIM(PAR.CGC_CPF)) AS CGC_CPF, "
				+ " CAB.NUMNOTA, "
				+ " PAR.RAZAOSOCIAL, "
				+ " ISNULL(EMP.AD_TXTEMAIL,' ') AS AD_TXTEMAIL, "
				+ " ISNULL(PAR.EMAILNFE, 'amanda.lima@alsolenergia.com.br') AS EMAILNFE, "
				+ " (SELECT CONTEUDO FROM TSIATA A WHERE A.CODATA = CAB.NUNOTA AND A.TIPO = 'N' AND A.TIPOCONTEUDO = 'P' AND DTALTER = (SELECT MAX(DTALTER) FROM TSIATA WHERE CODATA = A.CODATA AND TIPO = 'N' AND TIPOCONTEUDO = 'P')) AS CONTEUDO, "
				+ " (SELECT COUNT(*) FROM TSIATA  WHERE CODATA = CAB.NUNOTA AND TIPO = 'N' AND TIPOCONTEUDO = 'P') AS COUNT, "
				+ " (SELECT MAX(CODFILA)+1 FROM TMDFMG) AS ULTCOD, "
				+ " (SELECT MAX(NUANEXO)+1 FROM TMDAMG) AS ULTANEXO, "
				+ " (SELECT MAX(NUFIN) FROM TGFFIN WHERE NUNOTA = CAB.NUNOTA) AS NUFIN, "
				+ " CASE WHEN CAB.CODVEND = 93 THEN 'N' ELSE 'S' END AS NOSSONUM, "
				+ " GETDATE() AS DATA, "
				+ " ISNULL(EMP.AD_CODREL,116) AS AD_CODREL, ISNULL(EMP.AD_IMPFAT,'N') AS AD_IMPFAT, "
				+ " ISNULL(EMP.AD_IMPBOL,'N') AS AD_IMPBOL, ISNULL(EMP.AD_IMPPDF,'N') AS AD_IMPPDF "
				+ " FROM TGFCAB CAB, TGFPAR PAR, TSIEMP EMP "
				+ " WHERE CAB.CODEMP = EMP.CODEMP "
				+ " AND CAB.CODPARC = PAR.CODPARC " 
				+ " AND CAB.STATUSNOTA = 'L' " 
				+ " AND CAB.CODTIPOPER = 2122 "
				+ " AND ISNULL(AD_FATBOLENV,'N') = 'N'");
		
		try {
			
			BigDecimal pCodUsu = new BigDecimal(9);
			try {
				registry(pCodUsu);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			jdbc.openSession();
			JapeSessionContext.putProperty("usuario_logado", pCodUsu);
		    JapeSession.putProperty("impressaoNotas", Boolean.valueOf(true));
		    
			ResultSet rs = nativeSql.executeQuery(sql.toString());

			while (rs.next()) {

				BigDecimal nuAnexoBoleto = BigDecimal.ZERO;
				BigDecimal nuAnexoFatura = BigDecimal.ZERO;
				BigDecimal nuAnexoConsumo = BigDecimal.ZERO;
				BigDecimal ultCod = BigDecimal.ZERO;

				BigDecimal nuNota = rs.getBigDecimal("NUNOTA");
				// BigDecimal numNota = rs.getBigDecimal("NUMNOTA");
				BigDecimal nuFin = rs.getBigDecimal("NUFIN");
				Date dataAtual = rs.getDate("DATA");
				String razaoSocial = rs.getString("RAZAOSOCIAL");
				String emails = rs.getString("EMAILNFE");
				String pasta = rs.getString("PASTA");
				String cnpj = rs.getString("CGC_CPF");
				String recado = rs.getString("AD_TXTEMAIL");
				String nossoNumero = rs.getString("NOSSONUM");
				
				
				BigDecimal nurel = rs.getBigDecimal("AD_CODREL");
				String impFat = rs.getString("AD_IMPFAT");
				String impBol = rs.getString("AD_IMPBOL");
				String impPDF = rs.getString("AD_IMPPDF");
				
				System.out.println("THIAGO BONATTI - NUNOTA: " + nuNota);
				
				// Fatura
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				BigDecimal nroRelatorio = nurel;
				Map<String, Object> parameters = new HashMap<String, Object>();
				Report modeloImpressao = null;
				JasperPrint jasperPrint = null;
				parameters.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());
				parameters.put("NUNOTA", nuNota);
				modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);
				jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());
				byte[] fatura = JasperExportManager.exportReportToPdf(jasperPrint);

				// Boleto
				byte[] boleto = null;
				
				if (nossoNumero.equals("S")) {
					
					System.out.println("THIAGO BONATTI - boleto: " + nuFin);
					
					
				try {
					
				/*
				BoletoHelper.ConfiguracaoBoleto cfg = new BoletoHelper.ConfiguracaoBoleto();
				cfg.setGerarNumeroBoleto(true);
				cfg.setUsaContaBcoFinanceiros(true);
			    //cfg.setFinanceirosSelecionados(finsGerarBoleto);
			    cfg.setTipoSaidaBoleto(0);
			    cfg.setEnviarEmail(false);
				cfg.setFinanceirosSelecionados(Arrays.asList(new BigDecimal[] { nuFin }));
				BoletoHelper boletoHelper = new BoletoHelper();
				boletoHelper.gerarBoleto(cfg);
				boleto = boletoHelper.getBoletosPDF();
				*/
					
					BoletoHelper boletoHelper = new BoletoHelper();
				    boletoHelper.gerarBoletoPorNota(nuNota);
				    boleto = boletoHelper.getBoletosPDF();
				    
					
				} catch (IOException e) {
					boleto = null;
					e.printStackTrace();
					System.out.println("Fim ---- thiago bonatti - erro boleto");
				}
				}

				// Geração do e-mail
				String corpoemail = "<html> " + 
						"<head> " + 
						"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" /> " + 
						"  <title>Fatura</title> " + 
						"</head> " + 
						"<body> " + 
						recado +
						/*
						"<p><strong>COMUNICADO IMPORTANTE SOBRE SUA FATURA DE DEZEMBRO</strong></p> "+
						"<p>&nbsp;</p> "+
						"<p>Caro cliente,</p>  " +
						"<p>&nbsp;</p>  " +
						"<p>No m&ecirc;s de novembro, a CEMIG passou por uma altera&ccedil;&atilde;o no processo de medi&ccedil;&atilde;o de energia das usinas fotovoltaicas. Com essa altera&ccedil;&atilde;o, algumas contas de energia podem ter sofrido cobran&ccedil;as diferentes do habitual, acarretando em pagamentos indevidos.</p> "+
						"<p>&nbsp;</p> "+
						"<p>Caso voc&ecirc; j&aacute; tenha feito o pagamento da fatura que foi emitida indevidamente pela CEMIG, &eacute; um direito seu solicitar o reembolso.</p> "+
						"<p>&nbsp;</p> "+
						"<p>Em situa&ccedil;&otilde;es assim, &eacute; uma pr&aacute;tica comum da CEMIG gerar, automaticamente, o abatimento do valor direto na sua fatura. Se voc&ecirc; sofreu uma cobran&ccedil;a indevida e tiver direito ao reembolso, aparecer&aacute; da seguinte forma no campo <strong><u>VALORES FATURADOS </u>-></strong> &agrave; <strong><u>ABATIMENTOS E DEVOLU&Ccedil;&Otilde;ES,</u></strong> onde haver&aacute; um desconto do valor pago indevidamente &agrave; <strong>-></strong> ex.:<strong><u> RESTITUI&Ccedil;&Atilde;O DE PAGAMENTO</u>&nbsp; &ndash; <u> 17.473,04.</u></strong></p> "+
						"<p>&nbsp;</p> "+
						"<p>De acordo com a legisla&ccedil;&atilde;o (artigo 113 &sect; 4&ordm;; Resolu&ccedil;&atilde;o Normativa 414/2010), as empresas fornecedoras de energia el&eacute;trica podem gerar o abatimento direto na conta, mas tamb&eacute;m devem disponibilizar a op&ccedil;&atilde;o de reembolso atrav&eacute;s de cheques ou transfer&ecirc;ncias banc&aacute;rias.</p> "+
						"<p>&nbsp;</p> "+
						"<p>Se esse &eacute; o seu caso, recomendamos que acesse o link <a href=\"https://atende.cemig.com.br/ManifestacaoCliente\">https://atende.cemig.com.br/ManifestacaoCliente</a> e abra o chamado para solicita&ccedil;&atilde;o do reembolso via cheque ou transfer&ecirc;ncia banc&aacute;ria.</p> "+
						"<p>No m&ecirc;s de Dezembro a sua fatura ALSOL ser&aacute; enviada em seu e-mail, j&aacute; com os ajustes gerados pela CEMIG, junto com o seu relat&oacute;rio de consumo e, caso seja necess&aacute;rio, <b><u>o relat&oacute;rio de encontro de contas.</b></u></p> "+
						"<p>Sentimos pelo imprevisto, mas precisamos refor&ccedil;ar que esse acontecimento se deu por uma altera&ccedil;&atilde;o no processo de medi&ccedil;&atilde;o da CEMIG.</p> "+
						"<p>&nbsp;</p> "+
						"<p>A ALSOL est&aacute; sempre &agrave; sua disposi&ccedil;&atilde;o para esclarecimentos de quaisquer d&uacute;vidas, nos nossos canais de atendimento: (34) 2512-8820 ou (34) 99838-8044 e voc&ecirc; tamb&eacute;m pode entrar em contato atrav&eacute;s do e-mail <a href=\"mailto:sac@alsolenergia.com.br\">sac@alsolenergia.com.br</a></p> "+
						"<p>&nbsp;</p> "+
						"<p>Agradecemos a sua compreens&atilde;o. Conte sempre conosco.</p> "+
						"<p>&nbsp;</p> "+
						*/
						"  <p> Prezado Cliente, <b> " + razaoSocial + " </b></p> " + 
						"  <p>Encaminhamos em anexo fatura de loca&ccedil;&atilde;o, boleto e relat&oacute;rio de compensa&ccedil;&atilde;o de cr&eacute;ditos.</p> " + 
						"  <p>Qualquer d&uacute;vida entre em contato com a nossa equipe de p&oacute;s-venda pelos n&uacute;meros <b>(34) 2512-8820 / (34) 99838-8044</b>.</p> " + 
						"  <p>Estamos &agrave; disposi&ccedil;&atilde;o.</p> <br> " + 
						"  <p style=\"margin: 3px 0px 3px 0px;\"><b><span " + 
						"        style=\"font-size:14.0pt;font-family:&quot;Trebuchet MS&quot;,sans-serif;color:gray\">Depto Cobran&ccedil;a</span></b><span " + 
						"      style=\"font-size:14.0pt;font-family:&quot;Trebuchet MS&quot;,sans-serif;color:gray\"></span></p> " + 
						"  <p style=\"margin: 3px 0px 3px 0px;\"><span style=\"font-size:10.0pt;font-family:&quot;Trebuchet MS&quot;,sans-serif;color:gray\">34 2512-8813</p> " + 
						"  <p style=\"margin: 3px 0px 3px 0px;\"><span style=\"font-size:10.0pt;font-family:&quot;Trebuchet MS&quot;,sans-serif\"><a href=\"mailto:cobranca@alsolenergia.com.br\" target=\"_blank\"><span style=\"color:blue\">cobranca@alsolenergia.com.br</span></a></span><span></span></p> " + 
						"  </p> " + 
						"  <p><span style=\"font-size:10.0pt;font-family:&quot;Trebuchet MS&quot;,sans-serif;color:gray\"><a href=\"http://alsolenergia.com.br\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?q=http://alsolenergia.com.br&amp;source=gmail&amp;ust=1591882078383000&amp;usg=AFQjCNGu_XjS5gfcpPaosKAjrmR53HiqBA\">alsolenergia.com.br</a><u></u><u></u></span></p> " + 
						"  <p ><span><img border=\"0\" width=\"207\" height=\"31\" style=\"width:2.1562in;height:.3229in\"  src=\"https://images2.imgbox.com/f1/33/MjK6LXo9_o.png\"></span><span></span></p> " + 
						"</body> " + 
						"</html> ";
				char[] mensagem = corpoemail.toCharArray();
				String assunto = "Fatura";
				
				String arrayEmail[] = emails.split(";");

				for (String email : arrayEmail) {

					// Insere o email para fila de envio
					SessionHandle hnd = null;
					try {
						hnd = JapeSession.open();
						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
						DynamicVO dynamicVO = (DynamicVO) entityVO;
						dynamicVO.setProperty("ASSUNTO", assunto);
						dynamicVO.setProperty("DTENTRADA", dataAtual);
						dynamicVO.setProperty("STATUS", "Pendente");
						dynamicVO.setProperty("EMAIL", email);
						dynamicVO.setProperty("TENTENVIO", new BigDecimal(1));
						dynamicVO.setProperty("MENSAGEM", mensagem);
						dynamicVO.setProperty("NUCHAVE", nuNota);
						dynamicVO.setProperty("TIPOENVIO", "E");
						dynamicVO.setProperty("MAXTENTENVIO", new BigDecimal(3));
						dynamicVO.setProperty("CODSMTP", new BigDecimal(3));
						dynamicVO.setProperty("CODCON", new BigDecimal(0));
						PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", entityVO);
						DynamicVO save = (DynamicVO) createEntity.getValueObject();
						ultCod = save.asBigDecimal("CODFILA");
						
						//if (impBol.equals("S")) {
						
						if (nossoNumero.equals("S")) {
						// Cria anexo Boleto
						dwfFacade = EntityFacadeFactory.getDWFFacade();
						entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
						dynamicVO = (DynamicVO) entityVO;
						dynamicVO.setProperty("NOMEARQUIVO", "Boleto.pdf");
						dynamicVO.setProperty("TIPO", "application/pdf");
						dynamicVO.setProperty("ANEXO", boleto);
						createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
						save = (DynamicVO) createEntity.getValueObject();
						nuAnexoBoleto = save.asBigDecimal("NUANEXO");
						
						// Insere o anexo do boleto
						nativeSql.executeUpdate(" INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES " + "(" + ultCod + " , "
								+ nuAnexoBoleto + ")");
						}
						//}
						
					//	if (impFat.equals("S")) {

						// Cria anexo Fatura
						entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
						dynamicVO = (DynamicVO) entityVO;
						dynamicVO.setProperty("NOMEARQUIVO", "Fatura.pdf");
						dynamicVO.setProperty("TIPO", "application/pdf");
						dynamicVO.setProperty("ANEXO", fatura);
						createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
						save = (DynamicVO) createEntity.getValueObject();
						nuAnexoFatura = save.asBigDecimal("NUANEXO");
						
						// Insere o anexo da fatura
						nativeSql.executeUpdate(" INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES " + "(" + ultCod + " , "
								+ nuAnexoFatura + ")");

					//	}
						
					//	if (impPDF.equals("S")) {
							
						
						try {

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

							if (arquivos != null) {
								for (File arquivo : arquivos) {
									
									if (arquivo.getName().equals(cnpj + ".pdf")) {
										
										FileInputStream fileInputStream = null;
										byte[] bytesArray = null;

										try {

											bytesArray = new byte[(int) arquivo.length()];

											// read file into bytes[]
											fileInputStream = new FileInputStream(arquivo);
											fileInputStream.read(bytesArray);

										} catch (IOException e) {
											e.printStackTrace();
										} finally {
											if (fileInputStream != null) {
												try {
													fileInputStream.close();
												} catch (IOException e) {
													e.printStackTrace();
												}
											}

										}

										// Cria anexo do repositório
										entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
										dynamicVO = (DynamicVO) entityVO;
										dynamicVO.setProperty("NOMEARQUIVO", "Consumo.pdf");
										dynamicVO.setProperty("TIPO", "application/pdf");
										dynamicVO.setProperty("ANEXO", bytesArray);
										createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
										save = (DynamicVO) createEntity.getValueObject();
										nuAnexoConsumo = save.asBigDecimal("NUANEXO");

										// Insere o anexo do repositório
										nativeSql.executeUpdate(" INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES " + "("
												+ ultCod + " , " + nuAnexoConsumo + ")");
									}
								}
							} else {
								System.out.println("Parceiro sem arquivo de consumo anexado.");
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
						
					//	}

						// Atualiza a nota para não enviar mais esse email
						nativeSql.executeUpdate("UPDATE TGFCAB SET AD_FATBOLENV = 'S' WHERE NUNOTA = " + nuNota);

					} finally {
						JapeSession.close(hnd);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
	}
	
	protected void registry(BigDecimal codUsu) throws Exception {	

		oldAuthInfo = AuthenticationInfo.getCurrentOrNull();

		if (oldAuthInfo != null) {
			AuthenticationInfo.unregistry();
		}

		DynamicVO usuarioVO = (DynamicVO) EntityFacadeFactory.getDWFFacade()
				.findEntityByPrimaryKeyAsVO(DynamicEntityNames.USUARIO, new Object[] { codUsu });

		StringBuffer authID = new StringBuffer();
		authID.append(System.currentTimeMillis()).append(':').append(usuarioVO.asBigDecimal("CODUSU")).append(':')
				.append(this.hashCode());

		authInfo = new AuthenticationInfo(usuarioVO.asString("NOMEUSU"), usuarioVO.asBigDecimalOrZero("CODUSU"),
				usuarioVO.asBigDecimalOrZero("CODGRUPO"), new Integer(authID.toString().hashCode()));
		authInfo.makeCurrent();

		sctx.setAutentication(authInfo);
		sctx.makeCurrent();

		JapeSessionContext.putProperty("usuario_logado", authInfo.getUserID());
		JapeSessionContext.putProperty("emp_usu_logado", usuarioVO.asBigDecimal("CODEMP"));
		JapeSessionContext.putProperty("dh_atual", new Timestamp(System.currentTimeMillis()));
		JapeSessionContext.putProperty("d_atual", new Timestamp(TimeUtils.getToday()));
		JapeSessionContext.putProperty("usuarioVO", usuarioVO);
		JapeSessionContext.putProperty("authInfo", authInfo);

		// }
	}

}
