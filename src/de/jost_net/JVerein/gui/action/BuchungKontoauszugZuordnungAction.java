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

import java.rmi.RemoteException;

import de.jost_net.JVerein.Messaging.BuchungMessage;
import de.jost_net.JVerein.gui.control.BuchungsControl;
import de.jost_net.JVerein.gui.dialogs.BuchungsartZuordnungDialog;
import de.jost_net.JVerein.gui.dialogs.KontoauszugZuordnungDialog;
import de.jost_net.JVerein.rmi.Buchung;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Kontoauszugsinformationen zuordnen.
 */
public class BuchungKontoauszugZuordnungAction implements Action
{

  private BuchungsControl control;

  public BuchungKontoauszugZuordnungAction(BuchungsControl control)
  {
    this.control = control;
  }

  @Override
  public void handleAction(Object context) throws ApplicationException
  {
    if (context == null
        || (!(context instanceof Buchung) && !(context instanceof Buchung[])))
    {
      throw new ApplicationException("Keine Buchung(en) ausgewählt");
    }
    try
    {
      Buchung[] b = null;
      if (context instanceof Buchung)
      {
        b = new Buchung[1];
        b[0] = (Buchung) context;
      }
      if (context instanceof Buchung[])
      {
        b = (Buchung[]) context;
      }
      if (b == null)
      {
        return;
      }
      if (b.length == 0)
      {
        return;
      }
      if (b[0].isNewObject())
      {
        return;
      }
      try
      {
        KontoauszugZuordnungDialog kaz = new KontoauszugZuordnungDialog(
            BuchungsartZuordnungDialog.POSITION_MOUSE, b[0].getAuszugsnummer(),
            b[0].getBlattnummer());
        kaz.open();
        Integer auszugsnummer = kaz.getAuszugsnummerWert();
        Integer blattnummer = kaz.getBlattnummerWert();
        int counter = 0;

        for (Buchung buchung : b)
        {
          boolean protect = ((buchung.getAuszugsnummer() != null && buchung
              .getAuszugsnummer().intValue() > 0) || (buchung.getBlattnummer() != null && buchung
              .getBlattnummer().intValue() > 0))
              && !kaz.getOverride();
          if (protect)
          {
            counter++;
          }
          else
          {
            buchung.setAuszugsnummer(auszugsnummer);
            buchung.setBlattnummer(blattnummer);
            buchung.store();
            Application.getMessagingFactory().sendMessage(
                new BuchungMessage(buchung));
          }
        }
        control.getBuchungsList();
        String protecttext = "";
        if (counter > 0)
        {
          protecttext = String.format(
              ", {0} Buchungen wurden nicht überschrieben. ",
              new Object[] { counter + "" });
        }
        GUI.getStatusBar().setSuccessText(
            "Kontoauszugsinformationen zugeordnet" + protecttext);
      }
      catch (Exception e)
      {
        Logger.error("Fehler", e);
        GUI.getStatusBar().setErrorText(
            "Fehler bei der Zuordnung der Kontoauszugsinformationen");
        return;
      }
    }
    catch (RemoteException e)
    {
      String fehler = "Fehler beim Speichern.";
      GUI.getStatusBar().setErrorText(fehler);
      Logger.error(fehler, e);
    }
  }
}
