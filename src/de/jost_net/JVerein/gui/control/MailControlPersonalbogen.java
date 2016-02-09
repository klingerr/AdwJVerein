package de.jost_net.JVerein.gui.control;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

import org.apache.velocity.app.Velocity;

import com.itextpdf.text.DocumentException;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.gui.action.PersonalbogenAction;
import de.jost_net.JVerein.io.MailSender;
import de.jost_net.JVerein.io.Reporter;
import de.jost_net.JVerein.io.Adressbuch.Adressaufbereitung;
import de.jost_net.JVerein.rmi.MailAnhang;
import de.jost_net.JVerein.rmi.MailEmpfaenger;
import de.jost_net.JVerein.rmi.Mitglied;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.dialogs.SimpleDialog;
import de.willuhn.jameica.gui.dialogs.YesNoDialog;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

public class MailControlPersonalbogen extends MailControl {

	public MailControlPersonalbogen(AbstractView view) {
		super(view);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Button getMailSendButton()
	  {
	    Button b = new Button("speichern + senden", new Action()
	    {

	      @Override
	      public void handleAction(Object context) throws ApplicationException
	      {
	        try
	        {
	          int toBeSentCount = 0;
	          for (final MailEmpfaenger empf : getMail().getEmpfaenger())
	          {
	            if (empf.getVersand() == null)
	            {
	              toBeSentCount++;
	            }
	          }
	          if (toBeSentCount == 0)
	          {
	            SimpleDialog d = new SimpleDialog(SimpleDialog.POSITION_CENTER);
	            d.setTitle("Mail bereits versendet");
	            d.setText("Mail wurde bereits an alle Empfänger versendet!");
	            try
	            {
	              d.open();
	            }
	            catch (Exception e)
	            {
	              Logger.error("Fehler beim Nicht-Senden der Mail", e);
	            }
	            return;
	          }
	          if (toBeSentCount != getMail().getEmpfaenger().size())
	          {
	            YesNoDialog d = new YesNoDialog(YesNoDialog.POSITION_CENTER);
	            d.setTitle("Mail senden?");
	            d.setText("Diese Mail wurde bereits an "
	                + (getMail().getEmpfaenger().size() - toBeSentCount)
	                + " der gewählten Empfänger versendet. Wollen Sie diese Mail an alle weiteren "
	                + toBeSentCount + " Empfänger senden?");
	            try
	            {
	              Boolean choice = (Boolean) d.open();
	              if (!choice.booleanValue())
	                return;
	            }
	            catch (Exception e)
	            {
	              Logger.error(

	              "Fehler beim Senden der Mail", e);
	              return;
	            }
	          }
	          sendeMail(false);
	          // 20140917 RK: erfolgt hier innerhalb von "sendeMail", weil die 
	          // Personalbögen einzeln versendet aber zusammen gespeichert werden 
//	          handleStore(true);
	        }
	        catch (RemoteException e)
	        {
	          Logger.error(e.getMessage());
	          throw new ApplicationException("Fehler beim Senden der Mail");
	        }
	      }
	    }, null, true, "mail-message-new.png");
	    return b;
	  }

	
	  /**
	   * Versende Mail an Empfänger. Wenn erneutSenden==false wird Mail nur an
	   * Empfänger versendet, die Mail noch nicht erhalten haben.
	   */
	  private void sendeMail(final boolean erneutSenden) throws RemoteException
	  {
		Logger.info("sendeMail(" + erneutSenden + ")");
	    final String betr = getBetreffString();
	    final String txt = getTxtString()
	        + Einstellungen.getEinstellung().getMailSignatur();

	    BackgroundTask t = new BackgroundTask()
	    {

	      @Override
	      public void run(ProgressMonitor monitor)
	      {
	        try
	        {
	          MailSender sender = new MailSender(Einstellungen.getEinstellung()
	              .getSmtpServer(), Einstellungen.getEinstellung().getSmtpPort(),
	              Einstellungen.getEinstellung().getSmtpAuthUser(), Einstellungen
	                  .getEinstellung().getSmtpAuthPwd(), Einstellungen
	                  .getEinstellung().getSmtpFromAddress(), Einstellungen
	                  .getEinstellung().getSmtpFromAnzeigename(), Einstellungen
	                  .getEinstellung().getMailAlwaysBcc(), Einstellungen
	                  .getEinstellung().getMailAlwaysCc(), Einstellungen
	                  .getEinstellung().getSmtpSsl(), Einstellungen
	                  .getEinstellung().getSmtpStarttls(),
	              Einstellungen.getImapCopyData());

	          try {
					Velocity.init();
				} catch (Exception e1) {
	                Logger.error(e1.getMessage());
				}
	          Logger.debug("preparing velocity context");
	          monitor.setStatus(ProgressMonitor.STATUS_RUNNING);
	          monitor.setPercentComplete(0);
	          int zae = 0;
	          int sentCount = 0;
	          
	          Logger.info("Anzahl der Empfänger=" + getMail().getEmpfaenger().size());
	          
//			  Logger.info("getMail()1: " + getMail());
			  getMail().setBetreff(betr);
			  getMail().setTxt(txt);
			  getMail().setBearbeitung(new Timestamp(System.currentTimeMillis()));
			  getMail().setVersand(new Timestamp(System.currentTimeMillis()));
			  getMail().store();
//			  Logger.info("getMail()2: " + getMail());
          	  
          	  
	          for (final MailEmpfaenger empf : getMail().getEmpfaenger())
	          {
	        	TreeSet<MailAnhang> attachmentPerRecipient = new TreeSet<MailAnhang>();
	            MailAnhang mailAnhang = (MailAnhang) Einstellungen.getDBService().createObject(MailAnhang.class, null);
	        	mailAnhang.setMail(getMail());
	        	SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
	        	mailAnhang.setDateiname(empf.getMitglied().getName() + "_" + empf.getMitglied().getVorname() + "_" + sf.format(new Date()) + ".pdf");
	        	try {
					mailAnhang.setAnhang(generatePersonalbogen(new Mitglied[]{empf.getMitglied()}));
				} catch (Exception e) {
	                monitor.log(empf.getMitglied().getName() + ", " + empf.getMitglied().getVorname() + " - Erstellung des Personalbogens fehlgeschlagen (" + e.getMessage() + ")");
	                Logger.error(empf.getMitglied().getName() + ", " + empf.getMitglied().getVorname() + " - Erstellung des Personalbogens fehlgeschlagen (" + e.getMessage() + ")", e);
					continue;
				} 
	        	
	        	mailAnhang.store();
	        	
	        	attachmentPerRecipient.add(mailAnhang);
	        	getMail().setAnhang(attachmentPerRecipient);
	        	
	            EvalMail em = new EvalMail(empf);
	            if (erneutSenden || empf.getVersand() == null)
	            {
	              try {
					sender.sendMail(empf.getMailAdresse(), em.evalBetreff(betr),
					      em.evalText(txt), getMail().getAnhang());
					} catch (Exception e) {
		                monitor.log(empf.getMitglied().getName() + ", " + empf.getMitglied().getVorname() + ", " + empf.getMitglied().getEmail() + " - Versand des Personalbogens fehlgeschlagen (" + e.getMessage() + ")");
		                Logger.error(empf.getMitglied().getName() + ", " + empf.getMitglied().getVorname() + ", " + empf.getMitglied().getEmail() + " - Versand des Personalbogens fehlgeschlagen (" + e.getMessage() + ")", e);
						continue;
					} 
	              sentCount++;
	              monitor.log(empf.getMailAdresse() + " - versendet");
	              Logger.info(empf.getMitglied().getName() + "_" + empf.getMitglied().getVorname() + " - versendet");
	              // Nachricht wurde erfolgreich versendet; speicher Versand-Datum
	              // persistent.
	              empf.setVersand(new Timestamp(new Date().getTime()));
	              empf.setMail(getMail());
	              empf.store();
	              // aktualisiere TablePart getEmpfaenger() (zeige neues
	              // Versand-Datum)
	              GUI.startView(GUI.getCurrentView().getClass(), GUI
	                  .getCurrentView().getCurrentObject());
	            }
	            else
	            {
	              monitor.log(empf.getMailAdresse() + " - übersprungen");
	            }
	            zae++;
	            double proz = (double) zae
	                / (double) getMail().getEmpfaenger().size() * 100d;
	            monitor.setPercentComplete((int) proz);

	          }
//	          Logger.info("getMail()3: " + getMail());
	          
	          monitor.setPercentComplete(100);
	          monitor.setStatus(ProgressMonitor.STATUS_DONE);
	          monitor.setStatusText(MessageFormat.format(
	              "Anzahl verschickter Mails: {0}", sentCount + ""));
	          
	          handleStore(true);
	          monitor.setStatusText("Mail in DB gespeichert.");

	          GUI.getStatusBar().setSuccessText("Mail" + (sentCount > 1 ? "s" : "") + " verschickt");
	          GUI.getCurrentView().reload();
	        }
	        catch (ApplicationException ae)
	        {
	          Logger.error("", ae);
	          monitor.log(ae.getMessage());
	        }
	        catch (RemoteException re)
	        {
	          Logger.error("", re);
	          monitor.log(re.getMessage());
	        }
	      }

	      @Override
	      public void interrupt()
	      {
	        //
	      }

	      @Override
	      public boolean isInterrupted()
	      {
	        return false;
	      }
	    };
	    Application.getController().start(t);
	  }

	  
	  private byte[] generatePersonalbogen(Mitglied[] mitglied) throws DocumentException, ApplicationException, MalformedURLException, IOException{
		  ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
          Reporter rpt = new Reporter(pdfOutput, "", "Personalbogen", mitglied.length);
          PersonalbogenAction personalbogenAction = new PersonalbogenAction();

          Logger.info("Personalbogen mitglied.length=" +  mitglied.length);

          GUI.getStatusBar().setSuccessText("Auswertung gestartet");
          GUI.getCurrentView().reload();

          boolean first = true;

          for (Mitglied m : mitglied)
          {
            if (!first)
            {
              rpt.newPage();
            }
            first = false;

            rpt.add(
                    "Mitgliederdatenblatt" + " " + Adressaufbereitung.getVornameName(m),
                    14);

            personalbogenAction.generiereMitglied(rpt, m);
            personalbogenAction.generiereEigenschaften(rpt, m);

//                if (Einstellungen.getEinstellung().getVermerke()
//                    && ((m.getVermerk1() != null && m.getVermerk1().length() > 0) || (m
//                        .getVermerk2() != null && m.getVermerk2().length() > 0)))
//                {
//                  generiereVermerke(rpt, m);
//                }
//                if (Einstellungen.getEinstellung().getWiedervorlage())
//                {
//                  generiereWiedervorlagen(rpt, m);
//                }
            if (Einstellungen.getEinstellung().getLehrgaenge())
            {
            	personalbogenAction.generiereLehrgaenge(rpt, m);
            }
            personalbogenAction.generiereZusatzfelder(rpt, m);
            
            if (Einstellungen.getEinstellung().getArbeitseinsatz())
            {
            	personalbogenAction.generiereArbeitseinsaetze(rpt, m);
            }

            if (Einstellungen.getEinstellung().getZusatzbetrag())
            {
            	personalbogenAction.generiereZusatzbetrag(rpt, m);
            }
            
            personalbogenAction.generiereMitgliedskonto2(rpt, m);
          
          }

          rpt.close();

          Logger.info("Personalbogen size=" + pdfOutput.size());

		  return pdfOutput.toByteArray();
	  }
	  
}
