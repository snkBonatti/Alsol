package br.com.alsol.importxls;

//import java.text.ParseException;
//import java.math.BigDecimal;
//import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class InsertService implements AcaoRotinaJava {

	public void doAction(ContextoAcao ca) throws Exception {
		for (int i = 0; i < ca.getLinhas().length; i++) {
			Registro line = ca.getLinhas()[i];
			try {
				confirmInsert(ca, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void confirmInsert(ContextoAcao ca, Registro line) {

		DynamicVO itemVO;
		AuthenticationInfo auth = AuthenticationInfo.getCurrent();
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		BigDecimal codParc = (BigDecimal) line.getCampo("CODPARC");
		String nroInstalacao = (String) line.getCampo("NROINSTALACAO");
		String diaVenc = (String) line.getCampo("DIAVENC");
		BigDecimal credCompen = (BigDecimal) line.getCampo("CREDCOMPEN");
		BigDecimal vlrTot = (BigDecimal) line.getCampo("VLRTOT");
		BigDecimal numContrato = (BigDecimal) line.getCampo("NUMCONTRATO");
		BigDecimal nroUnico = (BigDecimal) line.getCampo("NROUNICO");
		BigDecimal sequencia = (BigDecimal) line.getCampo("SEQUENCIA");
		String gerado = (String) line.getCampo("GERADO");
		BigDecimal vlrUnit = vlrTot.divide(credCompen, 6, BigDecimal.ROUND_UP);
		BigDecimal nuNota = (BigDecimal) line.getCampo("NUNOTA");
		BigDecimal codCR = (BigDecimal) line.getCampo("CODCENCUS");
		QueryExecutor queryOrder = ca.getQuery();

		ca.setMensagemRetorno("Dados processados.");

		if (codParc.floatValue() > 0 && !nroInstalacao.equals("") && !diaVenc.equals("") && credCompen.floatValue() > 0
				&& vlrTot.floatValue() > 0 && numContrato.floatValue() > 0 && nuNota != null && !gerado.equals("S")) {

			try {

				JapeWrapper itemDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);

				itemDAO.deleteByCriteria("NUNOTA = ? " + "	AND CODPROD = 52560 " + " AND AD_NROINSTALACAO IS NULL",
						nuNota);

			} catch (Exception e1) {

				// ca.setMensagemRetorno("Erro ao deletar item.");
				e1.printStackTrace();
			}

			try {

				Collection<PrePersistEntityState> itensNota = new ArrayList<PrePersistEntityState>();

				itemVO = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance(DynamicEntityNames.ITEM_NOTA);

				itemVO.setProperty("CODPROD", new BigDecimal(52560));
				itemVO.setProperty("QTDNEG", credCompen);
				itemVO.setProperty("VLRUNIT", vlrUnit);
				itemVO.setProperty("USOPROD", "S");
				itemVO.setProperty("CODVOL", "UN");
				itemVO.setProperty("CODLOCALORIG", BigDecimal.ZERO);
				itemVO.setProperty("CONTROLE", " ");
				itemVO.setProperty("PERCDESC", BigDecimal.ZERO);
				itemVO.setProperty("VLRDESC", BigDecimal.ZERO);
				itemVO.setProperty("AD_NROINSTALACAO", nroInstalacao);

				PrePersistEntityState itePreState = PrePersistEntityState.build(dwfEntityFacade,
						DynamicEntityNames.ITEM_NOTA, itemVO);
				itensNota.add(itePreState);

				CACHelper cacHelper = new CACHelper();
				cacHelper.incluirAlterarItem(nuNota, auth, itensNota, true);

			} catch (Exception e1) {

				// ca.setMensagemRetorno("Erro ao incluir o item.");
				e1.printStackTrace();
			}

			try {

//				queryOrder.update("UPDATE TGFFIN SET VLRDESDOB = (SELECT SUM(VLRTOT) FROM GFITE WHERE NUNOTA = "
//						+ nuNota + "), DTVENC = DATEADD(DAY," + new BigDecimal(diaVenc).subtract(BigDecimal.ONE)
//						+ ", DATEADD(MONTH, DATEDIFF(MONTH,0,DTVENC),0)) WHERE NUNOTA = " + nuNota);
				
				queryOrder.update("UPDATE TGFFIN SET VLRDESDOB = (SELECT SUM(VLRTOT) FROM TGFITE WHERE NUNOTA = "
						+ nuNota + "), DTVENC = '" + diaVenc + "' , CODCENCUS = " + codCR
						+ " WHERE NUNOTA = " + nuNota);
				
				queryOrder.update("UPDATE TGFCAB SET CODCENCUS =  " + codCR 
						+ " WHERE NUNOTA = " + nuNota);

				queryOrder.update("UPDATE AD_IMPCONITE SET GERADO = 'S'" + " WHERE SEQUENCIA = " + sequencia
						+ " AND NROUNICO = " + nroUnico);

			} catch (Exception e1) {

				// ca.setMensagemRetorno("Erro ao atualizar financeiro.");
				e1.printStackTrace();
			}

		}

	}

}