/**********************************************************************
 * Copyright (c) by Heiner Jostkleigrewe
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See 
 *  the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, 
 * see <http://www.gnu.org/licenses/>.
 * 
 * heiner@jverein.de
 * www.jverein.de
 **********************************************************************/
package de.jost_net.JVerein.gui.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.gui.control.MitgliedskontoNode;
import de.jost_net.JVerein.io.FileViewer;
import de.jost_net.JVerein.io.Reporter;
import de.jost_net.JVerein.io.Adressbuch.Adressaufbereitung;
import de.jost_net.JVerein.keys.ArtBeitragsart;
import de.jost_net.JVerein.rmi.Arbeitseinsatz;
import de.jost_net.JVerein.rmi.Eigenschaften;
import de.jost_net.JVerein.rmi.Felddefinition;
import de.jost_net.JVerein.rmi.Lehrgang;
import de.jost_net.JVerein.rmi.Mitglied;
import de.jost_net.JVerein.rmi.Mitgliedfoto;
import de.jost_net.JVerein.rmi.Zusatzbetrag;
import de.jost_net.JVerein.rmi.Zusatzfelder;
import de.jost_net.JVerein.util.Dateiname;
import de.jost_net.JVerein.util.JVDateFormatTTMMJJJJ;
import de.willuhn.datasource.GenericIterator;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.datasource.rmi.DBService;
import de.willuhn.datasource.rmi.ResultSetExtractor;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

public class PersonalbogenAction implements Action
{
  private static final String INTERVALL_HALBJAEHRLICH = "halbjährlich";
/** aktuelles Halbjahr */
  private static final String TERM_TYPE_CURRENT = "current";
  /** letztes Halbjahr */
  private static final String TERM_TYPE_LAST = "last";
  private de.willuhn.jameica.system.Settings settings;
  DecimalFormat df = new DecimalFormat("0.00");
  // Präsentation auf Datenblatt
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
  // Abfrage der H2-DB
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

  private double summeHalbjahr = 0.0;
  private Date vonZahlungszeitraum = null;
		  
  @Override
  public void handleAction(Object context) throws ApplicationException
  {
    Mitglied[] m = null;
    if (context != null
        && (context instanceof Mitglied || context instanceof Mitglied[]))
    {
      if (context instanceof Mitglied)
      {
        m = new Mitglied[] { (Mitglied) context };
      }
      else if (context instanceof Mitglied[])
      {
        m = (Mitglied[]) context;
      }
      try
      {
        generierePersonalbogen(m);
      }
      catch (IOException e)
      {
        Logger.error("Fehler", e);
        throw new ApplicationException("Fehler bei der Aufbereitung", e);
      }
    }
    else
    {
      throw new ApplicationException("Kein Mitglied ausgewählt");
    }
  }

  private void generierePersonalbogen(Mitglied[] m) throws IOException
  {
    final Mitglied[] mitglied = m;
    FileDialog fd = new FileDialog(GUI.getShell(), SWT.SAVE);
    fd.setText("Ausgabedatei wählen.");

    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    String path = settings
        .getString("lastdir", System.getProperty("user.home"));
    if (path != null && path.length() > 0)
    {
      fd.setFilterPath(path);
    }
    fd.setFileName(new Dateiname("personalbogen", "", Einstellungen
        .getEinstellung().getDateinamenmuster(), "PDF").get());
    fd.setFilterExtensions(new String[] { "*.PDF" });

    String s = fd.open();
    if (s == null || s.length() == 0)
    {
      return;
    }
    if (!s.endsWith(".PDF"))
    {
      s = s + ".PDF";
    }
    final File file = new File(s);
    settings.setAttribute("lastdir", file.getParent());
    BackgroundTask t = new BackgroundTask()
    {

      @Override
      public void run(ProgressMonitor monitor) throws ApplicationException
      {
        try
        {
          Reporter rpt = new Reporter(new FileOutputStream(file), "",
              "Personalbogen", mitglied.length);

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

            generiereMitglied(rpt, m);
            generiereEigenschaften(rpt, m);

//            if (Einstellungen.getEinstellung().getVermerke()
//                && ((m.getVermerk1() != null && m.getVermerk1().length() > 0) || (m
//                    .getVermerk2() != null && m.getVermerk2().length() > 0)))
//            {
//              generiereVermerke(rpt, m);
//            }
//            if (Einstellungen.getEinstellung().getWiedervorlage())
//            {
//              generiereWiedervorlagen(rpt, m);
//            }
            if (Einstellungen.getEinstellung().getLehrgaenge())
            {
              generiereLehrgaenge(rpt, m);
            }
            generiereZusatzfelder(rpt, m);
            
            if (Einstellungen.getEinstellung().getArbeitseinsatz())
            {
              generiereArbeitseinsaetze(rpt, m);
            }

            if (Einstellungen.getEinstellung().getZusatzbetrag())
            {
              generiereZusatzbetrag(rpt, m);
            }
            
            generiereMitgliedskonto2(rpt, m);
          
          }
          rpt.close();
          FileViewer.show(file);
        }
        catch (Exception re)
        {
          Logger.error("Fehler", re);
          GUI.getStatusBar().setErrorText(re.getMessage());
          throw new ApplicationException(re);
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

  public void generiereMitglied(Reporter rpt, Mitglied m)
      throws DocumentException, MalformedURLException, IOException
  {
	Date letzteAenderung =  m.getLetzteAenderung();
    if (letzteAenderung == null) {
    	try {
			letzteAenderung = simpleDateFormat.parse("01.07.2014");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    rpt.add("Persönliche Angaben (Stand vom " + simpleDateFormat.format(letzteAenderung) + ")", 9);
	  
    rpt.addHeaderColumn("Feld", Element.ALIGN_LEFT, 65, BaseColor.LIGHT_GRAY);
    rpt.addHeaderColumn("Inhalt", Element.ALIGN_LEFT, 115, BaseColor.LIGHT_GRAY);
    rpt.createHeader();
    DBIterator it = Einstellungen.getDBService().createList(Mitgliedfoto.class);
    it.addFilter("mitglied = ?", new Object[] { m.getID() });
    if (it.size() > 0)
    {
      Mitgliedfoto foto = (Mitgliedfoto) it.next();
      if (foto.getFoto() != null)
      {
        rpt.addColumn("Foto", Element.ALIGN_LEFT);
        rpt.addColumn(foto.getFoto(), 100, 100, Element.ALIGN_RIGHT);
      }
    }
    if (Einstellungen.getEinstellung().getExterneMitgliedsnummer())
    {
      rpt.addColumn("Ext. Mitgliedsnummer", Element.ALIGN_LEFT);
      rpt.addColumn(
          m.getExterneMitgliedsnummer() != null ? m.getExterneMitgliedsnummer()
              + "" : "", Element.ALIGN_LEFT);
    }
    else
    {
      rpt.addColumn("Mitgliedsnummer", Element.ALIGN_LEFT);
      rpt.addColumn(m.getID(), Element.ALIGN_LEFT);
    }
    rpt.addColumn("Name, Vorname", Element.ALIGN_LEFT);
    rpt.addColumn(Adressaufbereitung.getNameVorname(m), Element.ALIGN_LEFT);
    rpt.addColumn("Anschrift", Element.ALIGN_LEFT);
    rpt.addColumn(Adressaufbereitung.getAnschrift(m), Element.ALIGN_LEFT);
    rpt.addColumn("Geburtsdatum", Element.ALIGN_LEFT);
    rpt.addColumn(m.getGeburtsdatum(), Element.ALIGN_LEFT);
    if (m.getSterbetag() != null)
    {
      rpt.addColumn("Sterbetag", Element.ALIGN_LEFT);
      rpt.addColumn(m.getSterbetag(), Element.ALIGN_LEFT);
    }
    rpt.addColumn("Geschlecht", Element.ALIGN_LEFT);
    rpt.addColumn(m.getGeschlecht(), Element.ALIGN_LEFT);
    rpt.addColumn("Kommunikation", Element.ALIGN_LEFT);
    String kommunikation = "";
    if (m.getTelefonprivat().length() != 0)
    {
      kommunikation += "privat: " + m.getTelefonprivat();
    }
    if (m.getTelefondienstlich().length() != 0)
    {
      if (kommunikation.length() > 0)
      {
        kommunikation += "\n";
      }
      kommunikation += "dienstlich: " + m.getTelefondienstlich();
    }
    if (m.getHandy().length() != 0)
    {
      if (kommunikation.length() > 0)
      {
        kommunikation += "\n";
      }
      kommunikation += "Handy: " + m.getHandy();
    }
    if (m.getEmail().length() != 0)
    {
      if (kommunikation.length() > 0)
      {
        kommunikation += "\n";
      }
      kommunikation += "Email: " + m.getEmail();
    }
    rpt.addColumn(kommunikation, Element.ALIGN_LEFT);
    if (m.getAdresstyp().getID().equals("1"))
    {
      rpt.addColumn("Eintritt", Element.ALIGN_LEFT);
      rpt.addColumn(m.getEintritt(), Element.ALIGN_LEFT);
      rpt.addColumn("Beitragsgruppe", Element.ALIGN_LEFT);
      String beitragsgruppe = m.getBeitragsgruppe().getBezeichnung()
          + " - "
//          + Einstellungen.DECIMALFORMAT.format(BeitragsUtil.getBeitrag(
//              Einstellungen.getEinstellung().getBeitragsmodel(),
//              m.getZahlungstermin(), m.getZahlungsrhythmus().getKey(),
//              m.getBeitragsgruppe(), new Date(), m.getEintritt(),
//              m.getAustritt())) + " EUR";
          + Einstellungen.DECIMALFORMAT.format(m.getBeitragsgruppe().getBetrag()) + " EUR pro Monat";
      rpt.addColumn(beitragsgruppe, Element.ALIGN_LEFT);

      if (Einstellungen.getEinstellung().getIndividuelleBeitraege())
      {
        rpt.addColumn("Individueller Beitrag", Element.ALIGN_LEFT);
        if (m.getIndividuellerBeitrag() > 0)
        {
          rpt.addColumn(
              Einstellungen.DECIMALFORMAT.format(m.getIndividuellerBeitrag())
                  + " EUR", Element.ALIGN_LEFT);
        }
        else
        {
          rpt.addColumn("", Element.ALIGN_LEFT);
        }
      }
      if (m.getBeitragsgruppe().getBeitragsArt() == ArtBeitragsart.FAMILIE_ZAHLER)
      {
        DBIterator itbg = Einstellungen.getDBService().createList(
            Mitglied.class);
        itbg.addFilter("zahlerid = ?", m.getID());
        rpt.addColumn("Zahler für", Element.ALIGN_LEFT);
        String zahltfuer = "";
        while (itbg.hasNext())
        {
          Mitglied mz = (Mitglied) itbg.next();
          if (zahltfuer.length() > 0)
          {
            zahltfuer += "\n";
          }
          zahltfuer += Adressaufbereitung.getNameVorname(mz);
        }
        rpt.addColumn(zahltfuer, Element.ALIGN_LEFT);
      }
      else if (m.getBeitragsgruppe().getBeitragsArt() == ArtBeitragsart.FAMILIE_ANGEHOERIGER)
      {
        Mitglied mfa = (Mitglied) Einstellungen.getDBService().createObject(
            Mitglied.class, m.getZahlerID() + "");
        rpt.addColumn("Zahler", Element.ALIGN_LEFT);
        rpt.addColumn(Adressaufbereitung.getNameVorname(mfa),
            Element.ALIGN_LEFT);
      }
      rpt.addColumn("Austritts-/Kündigungsdatum", Element.ALIGN_LEFT);
      String akdatum = "";
      if (m.getAustritt() != null)
      {
        akdatum += new JVDateFormatTTMMJJJJ().format(m.getAustritt());
      }
      if (m.getKuendigung() != null)
      {
        if (akdatum.length() != 0)
        {
          akdatum += " / ";
        }
        akdatum += new JVDateFormatTTMMJJJJ().format(m.getKuendigung());
      }
      rpt.addColumn(akdatum, Element.ALIGN_LEFT);
    }
//    rpt.addColumn("Zahlungsweg", Element.ALIGN_LEFT);
//    rpt.addColumn(Zahlungsweg.get(m.getZahlungsweg()), Element.ALIGN_LEFT);
//    if (m.getBic() != null && m.getBic().length() > 0
//        && m.getIban().length() > 0)
//    {
//      rpt.addColumn("Bankverbindung", Element.ALIGN_LEFT);
//      rpt.addColumn(m.getBic() + "/" + m.getIban(), Element.ALIGN_LEFT);
//    }
//    rpt.addColumn("Datum Erstspeicherung", Element.ALIGN_LEFT);
//    rpt.addColumn(m.getEingabedatum(), Element.ALIGN_LEFT);
//    rpt.addColumn("Datum letzte Änderung", Element.ALIGN_LEFT);
//    rpt.addColumn(m.getLetzteAenderung(), Element.ALIGN_LEFT);
    rpt.closeTable();
  }

  public void generiereZusatzbetrag(Reporter rpt, Mitglied m)
      throws RemoteException, DocumentException
  {
    DBIterator it;

    // wenn die Zusatzbeträge gebucht sind, d.h. ein Abrechnungslauf durchgeführt wurde
    it = Einstellungen.getDBService().createList(Zusatzbetrag.class);
    it.addFilter("mitglied = ?", new Object[] { m.getID() });
    it.addFilter("ausfuehrung >= ? and ausfuehrung < ?", new Object[] { sdf.format(getFirstDayOfCurrentHalfYear()), sdf.format(getLastDayOfCurrentHalfYear()) });
    it.setOrder("ORDER BY faelligkeit DESC");
    Logger.info("generiereZusatzbetrag(ausfuehrung) - it.size(): " + it.size());

    if (it.size() == 0)
    {
        // wenn die Zusatzbeträge noch nicht gebucht wurden, d.h. noch kein Abrechnungslauf stattgefunden hat
        it = Einstellungen.getDBService().createList(Zusatzbetrag.class);
        it.addFilter("mitglied = ?", new Object[] { m.getID() });
        it.addFilter("faelligkeit >= ? and faelligkeit < ?", new Object[] { sdf.format(getFirstDayOfCurrentHalfYear()), sdf.format(getLastDayOfCurrentHalfYear()) });
        it.setOrder("ORDER BY faelligkeit DESC");
        
        Logger.info("generiereZusatzbetrag(faelligkeit) - it.size(): " + it.size());
    }
    
      rpt.add(new Paragraph("Beitrag / Umlagen / Sanktionen"));
//      rpt.addHeaderColumn("Start", Element.ALIGN_LEFT, 30, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("nächste Fäll.", Element.ALIGN_LEFT, 23, BaseColor.LIGHT_GRAY);
//      rpt.addHeaderColumn("letzte Ausf.", Element.ALIGN_LEFT, 30, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Intervall", Element.ALIGN_LEFT, 23, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Ende", Element.ALIGN_LEFT, 23, BaseColor.LIGHT_GRAY);
//      rpt.addHeaderColumn("Kostenart", Element.ALIGN_LEFT, 30, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Buchungstext", Element.ALIGN_LEFT, 75, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Betrag / Monat", Element.ALIGN_RIGHT, 28, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Betrag / Halbjahr", Element.ALIGN_RIGHT, 28, BaseColor.LIGHT_GRAY);
      rpt.createHeader();
      
      int numberOfMonthsMembership = 6;
      if (m.getAustritt() != null) {
    	  numberOfMonthsMembership = m.getAustritt().getMonth() - getFirstDayOfCurrentHalfYear().getMonth() + 1;
      }
      
      summeHalbjahr = m.getBeitragsgruppe().getBetrag()*numberOfMonthsMembership;
      addRowZusatzbetrag(rpt, getFaelligkeit(), INTERVALL_HALBJAEHRLICH, null
    		  , "Mitgliedsbeitrag: " + m.getBeitragsgruppe().getBezeichnung(), summeHalbjahr);
      
      Zusatzbetrag tax = null;
      
      while (it.hasNext())
      {
        Zusatzbetrag z = (Zusatzbetrag) it.next();
        if (!z.getBuchungstext().toUpperCase().contains("Mehrwertsteuer".toUpperCase())
        		&& !z.getBuchungstext().toUpperCase().contains("MwSt".toUpperCase())) {
        	addRowZusatzbetrag(rpt, z.getFaelligkeit(), z.getIntervallText(), z.getEndedatum(), z.getBuchungstext(), z.getBetrag());
        	summeHalbjahr += z.getBetrag();
        } else {
        	tax = z;
        }
      }
      
      // Summenzeilen und MwSt. immer ausweisen
  	  rpt.addColumn("Gesamtbetrag (netto): ", Element.ALIGN_RIGHT, 4);
	  rpt.addColumn(df.format(summeHalbjahr/6.0) + " EUR", Element.ALIGN_RIGHT);
	  rpt.addColumn(df.format(summeHalbjahr) + " EUR", Element.ALIGN_RIGHT);
	  rpt.addColumn("zzgl. 7% MwSt. auf Bootsliegeentgelt: ", Element.ALIGN_RIGHT, 4);
	  rpt.addColumn(df.format(tax == null ? 0.0 : tax.getBetrag()/6.0) + " EUR", Element.ALIGN_RIGHT);
	  rpt.addColumn(df.format(tax == null ? 0.0 : tax.getBetrag()) + " EUR", Element.ALIGN_RIGHT, 4);
	  summeHalbjahr += tax == null ? 0.0 : tax.getBetrag();
	  rpt.addColumn("Gesamtbetrag (brutto): ", Element.ALIGN_RIGHT, 4);
	  rpt.addColumn(df.format(summeHalbjahr/6.0) + " EUR", Element.ALIGN_RIGHT);
	  rpt.addColumn(df.format(summeHalbjahr) + " EUR", Element.ALIGN_RIGHT);
//    }
    rpt.closeTable();
  }

	private Date getFaelligkeit() {
		Calendar cal = Calendar.getInstance();
		boolean isFirstTerm = cal.get(Calendar.MONTH) <= 6 ? true : false;
	
		cal.set(Calendar.DAY_OF_MONTH, 1);
		if (isFirstTerm) {
			cal.set(Calendar.MONTH, 3); // 3 = april
		} else {
			cal.set(Calendar.MONTH, 9); // 9 = october
		}
			
		Date faelligkeit =cal.getTime();
		return faelligkeit;
	}

	private void addRowZusatzbetrag(Reporter rpt, Date faelligkeit, String intervall, Date endedatum, String buchungstext, double halbjahresBetrag)
			throws RemoteException {
		rpt.addColumn(faelligkeit, Element.ALIGN_LEFT);
		rpt.addColumn(intervall, Element.ALIGN_LEFT);
		rpt.addColumn(endedatum, Element.ALIGN_LEFT);
		rpt.addColumn(buchungstext, Element.ALIGN_LEFT);
		double monatsBetrag = intervall.equals(INTERVALL_HALBJAEHRLICH) ? halbjahresBetrag/6.0 : 0.0;
		rpt.addColumn(df.format(monatsBetrag) + " EUR", Element.ALIGN_RIGHT);
		rpt.addColumn(df.format(halbjahresBetrag) + " EUR", Element.ALIGN_RIGHT);
	}

  
  public void generiereMitgliedskonto2(Reporter rpt, Mitglied m)
	      throws RemoteException, DocumentException
	  {
	  	MitgliedskontoNode currentNode = getTermBookings(m, TERM_TYPE_CURRENT);
	  	MitgliedskontoNode lastNode = getTermBookings(m, TERM_TYPE_LAST);
	  	
	    GenericIterator gi1 = currentNode.getChildren();
//	    if (gi1.size() > 0)
//	    {
	        rpt.add(new Paragraph("Mitgliedskontostand (Stichtag: " + new JVDateFormatTTMMJJJJ().format(getLastBookingDate()) + ")"));
		    rpt.addHeaderColumn("", Element.ALIGN_CENTER, 0, BaseColor.LIGHT_GRAY);
		    rpt.addHeaderColumn("", Element.ALIGN_CENTER, 0, BaseColor.LIGHT_GRAY);
		    rpt.addHeaderColumn("", Element.ALIGN_LEFT, 110, BaseColor.LIGHT_GRAY);
//		    rpt.addHeaderColumn("Datum", Element.ALIGN_CENTER, 20, BaseColor.LIGHT_GRAY);
//		    rpt.addHeaderColumn("Zweck", Element.ALIGN_LEFT, 70, BaseColor.LIGHT_GRAY);
//		    rpt.addHeaderColumn("Zahlungsweg", Element.ALIGN_LEFT, 20,  BaseColor.LIGHT_GRAY);
		    rpt.addHeaderColumn("Forderung", Element.ALIGN_RIGHT, 20, BaseColor.LIGHT_GRAY);
		    rpt.addHeaderColumn("Einzahlung", Element.ALIGN_RIGHT, 20, BaseColor.LIGHT_GRAY);
		    rpt.addHeaderColumn("Differenz", Element.ALIGN_RIGHT, 20,  BaseColor.LIGHT_GRAY);
		    rpt.createHeader();

		    // Summenzeile
		    generiereZeile(rpt, lastNode, null, true);
	    
		    // Summenzeile
//		    generiereZeile(rpt, currentNode, null, false);
      	    rpt.addColumn("Teilsummen aus aktuellem Halbjahr", Element.ALIGN_LEFT, 3);
		    rpt.addColumn(df.format(summeHalbjahr) + " EUR", Element.ALIGN_RIGHT);
		    rpt.addColumn(df.format((Double) currentNode.getAttribute("ist")) + " EUR", Element.ALIGN_RIGHT);
		    double diffCurrentTerm = (Double) currentNode.getAttribute("ist") - summeHalbjahr;
		    rpt.addColumn(df.format(diffCurrentTerm) + " EUR", Element.ALIGN_RIGHT);

		    // Zu zahlender Betrag
//		    Double payAmount = (Double) lastNode.getAttribute("differenz") + (Double) currentNode.getAttribute("differenz");
		    Double payAmount = (Double) lastNode.getAttribute("differenz") + diffCurrentTerm;
		    generiereZeile(rpt, currentNode, payAmount, false);
//	    
//			// Summe zurücksetzen, sonst Übertrag zum nächsten Mitglied
//			summeHalbjahr = 0.0;

//	    }
	    rpt.closeTable();
	    rpt.add("Bitte die Angaben genau prüfen! Es können unbeabsichtigte Fehler aufgetreten sein.", 9);
	    
	    rpt.add("\nBeachte unser neues Beitragskonto:                                               Rückfragen an: Ralf Klinger" +
	            "\n   IBAN: DE83 1203 0000 1020 2056 45                                                             email: ralf.klinger@adw-zeuthen.de" +
	    		"\n   BIC: BYLADEM 1001                                                                                      mobil: 0172/60 16 796" +
	    		"\n   Deutsche Kreditbank AG", 9);
	  }

  private Date getFirstDayOfCurrentHalfYear() {
		Calendar cal = Calendar.getInstance();
		boolean isFirstTerm = cal.get(Calendar.MONTH) <= 6 ? true : false;
		Date von = null;		

		// 1. Halbjahr des aktuellen Jahres
		if (isFirstTerm) {
			cal.set(Calendar.MONTH, 0); // 0 = januray
			cal.set(Calendar.DAY_OF_MONTH, 1);
			von = cal.getTime();
		// 2. Halbjahr des aktuellen Jahres
		} else {
			cal.set(Calendar.MONTH, 6); // 6 = july
			cal.set(Calendar.DAY_OF_MONTH, 1);
			von = cal.getTime();
		}

  	return von;
  }

  private Date getLastDayOfCurrentHalfYear() {
		Calendar cal = Calendar.getInstance();
		boolean isFirstTerm = cal.get(Calendar.MONTH) <= 6 ? true : false;
		Date bis = null;		

		// 1. Halbjahr des aktuellen Jahres
		if (isFirstTerm) {
			cal.set(Calendar.MONTH, 5); // 5 = june
			cal.set(Calendar.DAY_OF_MONTH, 30);
			bis = cal.getTime();
		// 2. Halbjahr des aktuellen Jahres
		} else {
			cal.set(Calendar.MONTH, 11); // 11 = december
			cal.set(Calendar.DAY_OF_MONTH, 31);
			bis = cal.getTime();
		}

  	return bis;
  }

	private MitgliedskontoNode getTermBookings(Mitglied m, String termType) throws RemoteException {
		Calendar cal = Calendar.getInstance();
		boolean isFirstTerm = cal.get(Calendar.MONTH) <= 6 ? true : false;
		Date von = null, bis = null;		

		if (termType.equals(TERM_TYPE_CURRENT)) {
			// 1. Halbjahr des aktuellen Jahres
			if (isFirstTerm) {
				cal.set(Calendar.MONTH, 0); // 0 = januray
				cal.set(Calendar.DAY_OF_MONTH, 1);
				von = cal.getTime();
				cal.set(Calendar.MONTH, 5); // 5 = june
				cal.set(Calendar.DAY_OF_MONTH, 30);
				bis = cal.getTime();
			// 2. Halbjahr des aktuellen Jahres
			} else {
				cal.set(Calendar.MONTH, 6); // 6 = july
				cal.set(Calendar.DAY_OF_MONTH, 1);
				von = cal.getTime();
				cal.set(Calendar.MONTH, 11); // 11 = december
				cal.set(Calendar.DAY_OF_MONTH, 31);
				bis = cal.getTime();
			}
		} else if(termType.equals(TERM_TYPE_LAST)) {
			// 2. Halbjahr des letzten Jahres
			if (isFirstTerm) {
				cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1); // letztes Jahr
				cal.set(Calendar.MONTH, 11); // 11 = december
				cal.set(Calendar.DAY_OF_MONTH, 31);
				bis = cal.getTime();
			// 1. Halbjahr des aktuellen Jahres
			} else {
				cal.set(Calendar.MONTH, 5); // 5 = june
				cal.set(Calendar.DAY_OF_MONTH, 30);
				bis = cal.getTime();
			}
			// Alle Einzahlungen seit 1.7.2014 berücksichtigen
			cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2014);
			cal.set(Calendar.MONTH, 6); // 6 = juli
			cal.set(Calendar.DAY_OF_MONTH, 1);
			von = cal.getTime();
			this.vonZahlungszeitraum = von;
		}
		
		MitgliedskontoNode node = new MitgliedskontoNode(m, von, bis);
		return node;
	}
	
	  private void generiereZeile(Reporter rpt, MitgliedskontoNode node, Double payAmount, boolean isLastTerm)
	  {
		if (payAmount != null) {
	        if (payAmount >= 0) {
		        rpt.addColumn("Vorhandenes Guthaben:", Element.ALIGN_RIGHT, 5);
	        	rpt.addColumn(df.format(payAmount) + " EUR", Element.ALIGN_RIGHT);
	        } else {
		        rpt.addColumn("Zu zahlende Gesamtsumme:", Element.ALIGN_RIGHT, 5);
	        	rpt.addColumn(df.format(payAmount * -1.0) + " EUR", Element.ALIGN_RIGHT);
	        }
		} else {
		    switch (node.getType())
		    {
		      case MitgliedskontoNode.MITGLIED:
		    	if (isLastTerm) {
//			    	  rpt.addColumn("Übertrag aus Zahlungszeiträumen seit " + simpleDateFormat.format(vonZahlungszeitraum), Element.ALIGN_LEFT, 3);
			    	  rpt.addColumn("Übertrag aus vorigen Zahlungszeiträumen", Element.ALIGN_LEFT, 3);
		    	} else {
		    	  rpt.addColumn("Teilsummen aus aktuellem Halbjahr", Element.ALIGN_LEFT, 3);
		    	}
		        break;
		      case MitgliedskontoNode.SOLL:
		        rpt.addColumn("Soll", Element.ALIGN_CENTER);
			    rpt.addColumn((Date) node.getAttribute("datum"), Element.ALIGN_CENTER);
			    rpt.addColumn((String) node.getAttribute("zweck1"), Element.ALIGN_LEFT);
		        break;
		      case MitgliedskontoNode.IST:
		        rpt.addColumn("Haben", Element.ALIGN_RIGHT);
			    rpt.addColumn((Date) node.getAttribute("datum"), Element.ALIGN_CENTER);
			    rpt.addColumn((String) node.getAttribute("zweck1"), Element.ALIGN_LEFT);
		        break;
		    }
	    
		    rpt.addColumn(" ", Element.ALIGN_RIGHT);
		    rpt.addColumn(" ", Element.ALIGN_RIGHT);
		    rpt.addColumn(df.format((Double) node.getAttribute("differenz")) + " EUR", Element.ALIGN_RIGHT);
		}
	  }
  
  /** Liefert das Datum der letzten Buchung. */
  private Date getLastBookingDate()
  {
    try {
	    DBService service = Einstellungen.getDBService();
	    String sql = "SELECT max(DATUM) FROM BUCHUNG";
	
	    ResultSetExtractor rs = new ResultSetExtractor()
	    {
	      @Override
	      public Object extract(ResultSet rs) throws SQLException
	      {
	        if (!rs.next())
	        {
	          return new Date();
	        }
	        return rs.getDate(1);
	      }
	    };
		return (Date) service.execute(sql, new Object[] {}, rs);
	} catch (RemoteException e) {
        Logger.error("Fehler bei Ermittlung des letzten Buchungsdatums!", e);
	}
	return new Date();
  }
	  
//  private void generiereVermerke(Reporter rpt, Mitglied m)
//      throws DocumentException, RemoteException
//  {
//    rpt.add(new Paragraph("Vermerke"));
//    rpt.addHeaderColumn("Text", Element.ALIGN_LEFT, 100, BaseColor.LIGHT_GRAY);
//    rpt.createHeader();
//    if (m.getVermerk1() != null && m.getVermerk1().length() > 0)
//    {
//      rpt.addColumn(m.getVermerk1(), Element.ALIGN_LEFT);
//    }
//    if (m.getVermerk2() != null && m.getVermerk2().length() > 0)
//    {
//      rpt.addColumn(m.getVermerk2(), Element.ALIGN_LEFT);
//    }
//    rpt.closeTable();
//
//  }
//
//  private void generiereWiedervorlagen(Reporter rpt, Mitglied m)
//      throws RemoteException, DocumentException
//  {
//    DBIterator it = Einstellungen.getDBService()
//        .createList(Wiedervorlage.class);
//    it.addFilter("mitglied = ?", new Object[] { m.getID() });
//    it.setOrder("order by datum desc");
//    if (it.size() > 0)
//    {
//      rpt.add(new Paragraph("Wiedervorlage"));
//      rpt.addHeaderColumn("Datum", Element.ALIGN_LEFT, 50, BaseColor.LIGHT_GRAY);
//      rpt.addHeaderColumn("Vermerk", Element.ALIGN_LEFT, 100,
//          BaseColor.LIGHT_GRAY);
//      rpt.addHeaderColumn("Erledigung", Element.ALIGN_LEFT, 50,
//          BaseColor.LIGHT_GRAY);
//      rpt.createHeader();
//      while (it.hasNext())
//      {
//        Wiedervorlage w = (Wiedervorlage) it.next();
//        rpt.addColumn(w.getDatum(), Element.ALIGN_LEFT);
//        rpt.addColumn(w.getVermerk(), Element.ALIGN_LEFT);
//        rpt.addColumn(w.getErledigung(), Element.ALIGN_LEFT);
//      }
//    }
//    rpt.closeTable();
//
//  }

  public void generiereLehrgaenge(Reporter rpt, Mitglied m)
      throws RemoteException, DocumentException
  {
    DBIterator it = Einstellungen.getDBService().createList(Lehrgang.class);
    it.addFilter("mitglied = ?", new Object[] { m.getID() });
    it.setOrder("order by von");
    if (it.size() > 0)
    {
      rpt.add(new Paragraph("Lehrgänge"));
      rpt.addHeaderColumn("Lehrgangsart", Element.ALIGN_LEFT, 50,
          BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("am/vom", Element.ALIGN_LEFT, 30,
          BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("bis", Element.ALIGN_LEFT, 30, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Veranstalter", Element.ALIGN_LEFT, 60,
          BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Ergebnis", Element.ALIGN_LEFT, 60,
          BaseColor.LIGHT_GRAY);
      rpt.createHeader();
      while (it.hasNext())
      {
        Lehrgang l = (Lehrgang) it.next();
        rpt.addColumn(l.getLehrgangsart().getBezeichnung(), Element.ALIGN_LEFT);
        rpt.addColumn(l.getVon(), Element.ALIGN_LEFT);
        rpt.addColumn(l.getBis(), Element.ALIGN_LEFT);
        rpt.addColumn(l.getVeranstalter(), Element.ALIGN_LEFT);
        rpt.addColumn(l.getErgebnis(), Element.ALIGN_LEFT);
      }
    }
    rpt.closeTable();
  }

  public void generiereZusatzfelder(Reporter rpt, Mitglied m)
      throws RemoteException, DocumentException
  {
    DBIterator it = Einstellungen.getDBService().createList(Felddefinition.class);
    it.setOrder("order by label");
    if (it.size() > 0)
    {
      Map<String, String> zusatzfelder = new TreeMap<String, String>();
      
      while (it.hasNext())
      {
        Felddefinition fd = (Felddefinition) it.next();
        DBIterator it2 = Einstellungen.getDBService().createList(Zusatzfelder.class);
        it2.addFilter("mitglied = ? and felddefinition = ?", new Object[] { m.getID(), fd.getID() });

        if (it2.size() > 0)
        {
          Zusatzfelder zf = (Zusatzfelder) it2.next();
          // alle Zusatzfelder in einer Map zwischenspeichern
          zusatzfelder.put(fd.getLabel(), zf.getString());
        }
      }
       
      if (!zusatzfelder.isEmpty()) {
	      rpt.add(new Paragraph("Zusatzfelder"));
	      rpt.addHeaderColumn("Feld", Element.ALIGN_LEFT, 50, BaseColor.LIGHT_GRAY);
	      rpt.addHeaderColumn("Inhalt", Element.ALIGN_LEFT, 40, BaseColor.LIGHT_GRAY);
	      rpt.addHeaderColumn("Feld", Element.ALIGN_LEFT, 50, BaseColor.LIGHT_GRAY);
	      rpt.addHeaderColumn("Inhalt", Element.ALIGN_LEFT, 40, BaseColor.LIGHT_GRAY);
	      rpt.createHeader();
        		  
	      for (Map.Entry<String, String> zusatzfeld : zusatzfelder.entrySet()) {
	    	  if (zusatzfeld.getKey().contains("führerschein")
	    			|| (m.getBeitragsgruppe().getBezeichnung().contains("Kind")
	    				&& zusatzfeld.getKey().contains("Kind"))
	    			|| (!m.getBeitragsgruppe().getBezeichnung().contains("Kind")
		    				&& !zusatzfeld.getKey().contains("Kind"))) {
	    		  rpt.addColumn(zusatzfeld.getKey(), Element.ALIGN_LEFT);
	    		  rpt.addColumn(zusatzfeld.getValue(), Element.ALIGN_LEFT);
	    	  }
	      }
	      rpt.closeTable();
      }
    }
  }

  public void generiereEigenschaften(Reporter rpt, Mitglied m)
      throws RemoteException, DocumentException
  {
    ResultSetExtractor rs = new ResultSetExtractor()
    {

      @Override
      public Object extract(ResultSet rs) throws SQLException
      {
        List<String> ids = new ArrayList<String>();
        while (rs.next())
        {
          ids.add(rs.getString(1));
        }
        return ids;
      }
    };
    String sql = "select eigenschaften.id from eigenschaften, eigenschaft "
        + "where eigenschaften.eigenschaft = eigenschaft.id and mitglied = ? "
        + "order by eigenschaft.bezeichnung";
    @SuppressWarnings("unchecked")
    ArrayList<String> idliste = (ArrayList<String>) Einstellungen
        .getDBService().execute(sql, new Object[] { m.getID() }, rs);
    if (idliste.size() > 0)
    {
//      rpt.add(new Paragraph("Eigenschaften"));
//        rpt.addHeaderColumn("Sparte / Funktion", Element.ALIGN_LEFT, 65, BaseColor.LIGHT_GRAY);
//        rpt.addHeaderColumn("Wert", Element.ALIGN_LEFT, 115, BaseColor.LIGHT_GRAY);
        rpt.addHeaderColumn("", Element.ALIGN_LEFT, 65, BaseColor.LIGHT_GRAY);
        rpt.addHeaderColumn("", Element.ALIGN_LEFT, 115, BaseColor.LIGHT_GRAY);
      rpt.createHeader();
      
      for (String id : idliste)
      {
        DBIterator it = Einstellungen.getDBService().createList(Eigenschaften.class);
        it.addFilter("id = ?", new Object[] { id });
        while (it.hasNext())
        {
          Eigenschaften ei = (Eigenschaften) it.next();
          rpt.addColumn(ei.getEigenschaft().getEigenschaftGruppe().getBezeichnung(), Element.ALIGN_LEFT);
          rpt.addColumn(ei.getEigenschaft().getBezeichnung(), Element.ALIGN_LEFT);
        }
      }
      rpt.closeTable();
    }
  }

  public void generiereArbeitseinsaetze(Reporter rpt, Mitglied m)
      throws RemoteException, DocumentException
  {
    DBIterator it = Einstellungen.getDBService().createList(
        Arbeitseinsatz.class);
//    it.addFilter("mitglied = ?", new Object[] { m.getID() });  
    Calendar cal = Calendar.getInstance();
	cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1); // letztes Jahr
	cal.set(Calendar.MONTH, 0); // 0 = january
	cal.set(Calendar.DAY_OF_MONTH, 1);

    it.addFilter("mitglied = ? and datum >= ?", new Object[] { m.getID(), sdf.format(cal.getTime()) });
    it.setOrder("ORDER BY datum");
    if (it.size() > 0)
    {
      rpt.add(new Paragraph("Arbeitseinsätze (seit " + simpleDateFormat.format(cal.getTime()) + ")"));
      rpt.addHeaderColumn("Stand", Element.ALIGN_LEFT, 30, BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("geleistete Arbeitsstunden", Element.ALIGN_LEFT, 30,
          BaseColor.LIGHT_GRAY);
      rpt.addHeaderColumn("Bemerkung", Element.ALIGN_LEFT, 90,
          BaseColor.LIGHT_GRAY);
      rpt.createHeader();
      while (it.hasNext())
      {
        Arbeitseinsatz ae = (Arbeitseinsatz) it.next();
        rpt.addColumn(ae.getDatum(), Element.ALIGN_LEFT);
        rpt.addColumn(ae.getStunden());
        rpt.addColumn(ae.getBemerkung(), Element.ALIGN_LEFT);
      }
    }
    rpt.closeTable();
  }
}
