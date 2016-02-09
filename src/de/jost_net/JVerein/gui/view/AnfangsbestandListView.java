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
package de.jost_net.JVerein.gui.view;

import de.jost_net.JVerein.gui.action.AnfangsbestandNeuAction;
import de.jost_net.JVerein.gui.action.DokumentationAction;
import de.jost_net.JVerein.gui.control.AnfangsbestandControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.ButtonArea;

public class AnfangsbestandListView extends AbstractView
{

  @Override
  public void bind() throws Exception
  {
    GUI.getView().setTitle("Anfangsbest�nde");

    AnfangsbestandControl control = new AnfangsbestandControl(this);

    control.getAnfangsbestandList().paint(this.getParent());

    ButtonArea buttons = new ButtonArea();
    buttons.addButton("Hilfe", new DokumentationAction(),
        DokumentationUtil.ANFANGSBESTAENDE, false, "help-browser.png");
    buttons.addButton("neu", new AnfangsbestandNeuAction(), null, true,
        "document-new.png");
    buttons.paint(this.getParent());
  }

  @Override
  public String getHelp()
  {
    return "<form><p><span color=\"header\" font=\"header\">Anfangsbest�nde</span></p>"
        + "<p>F�r jedes Konto ist zu Beginn des Gesch�ftsjahres der Anfangsbestand zu "
        + "speichern. Die Buchungen sind in chronologisch korrekter Reihenfolge vorzunehmen.</p> "
        + "<p>Durch einen Doppelklick kann ein Anfangsbestand korrigiert werden. Mit einem "
        + "Klick auf neu wird ein neuer Anfangsbestand aufgenommen. Durch einen Rechtsklick "
        + "�ffnet sich ein Kontextmen�, mit dem ein Anfangsbestand gel�scht werden kann.</p>"
        + "</form>";
  }
}
