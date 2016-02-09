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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import de.jost_net.JVerein.rmi.MailAnhang;
import de.jost_net.JVerein.rmi.Mitglied;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

/**
 * Speichern aller ausgewählten Mailanhänge im Dateisystem. Jeder Anhang wird
 * eine eigene Datei gemäß hinterlegtem Dateinamen.
 */
public class MailAnhangSpeichernAction implements Action
{
  private de.willuhn.jameica.system.Settings settings;
	
  @Override
  public void handleAction(Object context) throws ApplicationException
  {
    MailAnhang[] m = null;
    if (context != null
        && (context instanceof MailAnhang) || context instanceof MailAnhang[])
    {
      if (context instanceof MailAnhang)
      {
        m = new MailAnhang[] { (MailAnhang) context };
      }
      else if (context instanceof MailAnhang[])
      {
        m = (MailAnhang[]) context;
      }
      try
      {
        saveMailAnhang(m);
      }
      catch (IOException e)
      {
        Logger.error("Fehler", e);
        throw new ApplicationException("Fehler bei der Aufbereitung", e);
      }
    }
    else
    {
      throw new ApplicationException("Kein Mailanhang ausgewählt");
    }
  }

  private void saveMailAnhang(MailAnhang[] m) throws IOException
  {
    final MailAnhang[] mailAnhang = m;
    DirectoryDialog dd = new DirectoryDialog(GUI.getShell(), SWT.SAVE);
//    FileDialog fd = new FileDialog(GUI.getShell(), SWT.SAVE);
    dd.setText("Verzeichnis wählen.");

    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    String path = settings.getString("lastdir", System.getProperty("user.home"));
    if (path != null && path.length() > 0)
    {
      dd.setFilterPath(path);
    }
    
    final String s = dd.open();
    if (s == null || s.length() == 0)
    {
      Logger.error("Fehler: Ausgewähltes Verzeichnis ist leer: >" + s + "<");
      return;
    }
//    Logger.info("Ausgewähltes Verzeichnis: >" + s + "<");
    
    BackgroundTask t = new BackgroundTask()
    {

      @Override
      public void run(ProgressMonitor monitor) throws ApplicationException
      {
        try
        {
          GUI.getStatusBar().setSuccessText("Speichern der Anhänge gestartet");
          GUI.getCurrentView().reload();
          
          // jeden Anhang in einer eigenen Datei im oben ausgewählten Verzeichnis speichern
          for (MailAnhang ma : mailAnhang) {
//        	    Logger.info("Zu speichernder Anhang: >" + s + File.separator + ma.getDateiname() + "<");
            	final File file = new File(s + File.separator + ma.getDateiname());
            	settings.setAttribute("lastdir", file.getParent());
            	FileOutputStream fos = new FileOutputStream(file);
            	fos.write(ma.getAnhang(), 0, ma.getAnhang().length);
            	fos.close();
          }

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
}
